#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <stdatomic.h>
#include <android/log.h>

#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/error.h>
#include <libavutil/mathematics.h>

#define TAG "orangeffmpegkit"

static atomic_int g_cancelled = 0;

typedef struct InputOpenOptions {
    char *protocol_whitelist;
    char *allowed_extensions;
    char *headers;
    char *cookies;
    char *user_agent;
    char *referer;
} InputOpenOptions;

static void free_input_open_options(InputOpenOptions *options) {
    if (options == NULL) {
        return;
    }
    free(options->protocol_whitelist);
    free(options->allowed_extensions);
    free(options->headers);
    free(options->cookies);
    free(options->user_agent);
    free(options->referer);
    memset(options, 0, sizeof(InputOpenOptions));
}

static char *normalize_headers(const char *headers) {
    if (headers == NULL || headers[0] == '\0') {
        return NULL;
    }
    size_t len = strlen(headers);
    int ends_with_crlf = len >= 2 && headers[len - 2] == '\r' && headers[len - 1] == '\n';
    size_t append_len = ends_with_crlf ? 0 : 2;
    char *result = (char *) malloc(len + append_len + 1);
    if (result == NULL) {
        return NULL;
    }
    memcpy(result, headers, len);
    if (!ends_with_crlf) {
        result[len] = '\r';
        result[len + 1] = '\n';
    }
    result[len + append_len] = '\0';
    return result;
}


static void trim_line(char *s) {
    if (s == NULL) {
        return;
    }
    size_t len = strlen(s);
    while (len > 0 && (s[len - 1] == '\n' || s[len - 1] == '\r' || s[len - 1] == ' ' || s[len - 1] == '\t')) {
        s[--len] = '\0';
    }
    size_t start = 0;
    while (s[start] == ' ' || s[start] == '\t') {
        start++;
    }
    if (start > 0) {
        memmove(s, s + start, strlen(s + start) + 1);
    }
}

static int is_uri_or_abs_path(const char *path) {
    if (path == NULL || path[0] == '\0') {
        return 0;
    }
    if (path[0] == '/') {
        return 1;
    }
    return strstr(path, "://") != NULL;
}

static int copy_file_to_stream(const char *file_path, FILE *out, long *total_written) {
    FILE *in = fopen(file_path, "rb");
    if (in == NULL) {
        return -1;
    }

    char buffer[8192];
    size_t read_count;
    while ((read_count = fread(buffer, 1, sizeof(buffer), in)) > 0) {
        if (fwrite(buffer, 1, read_count, out) != read_count) {
            fclose(in);
            return -1;
        }
        *total_written += (long) read_count;
    }

    fclose(in);
    return 0;
}

static int merge_local_m3u8(const char *m3u8_path, const char *output_path) {
    if (m3u8_path == NULL || output_path == NULL) {
        return -3;
    }

    FILE *m3u8 = fopen(m3u8_path, "r");
    if (m3u8 == NULL) {
        return -3;
    }

    FILE *output = fopen(output_path, "wb");
    if (output == NULL) {
        fclose(m3u8);
        return -3;
    }

    char base_dir[PATH_MAX];
    memset(base_dir, 0, sizeof(base_dir));
    strncpy(base_dir, m3u8_path, sizeof(base_dir) - 1);
    char *last_slash = strrchr(base_dir, '/');
    if (last_slash != NULL) {
        *last_slash = '\0';
    } else {
        strncpy(base_dir, ".", sizeof(base_dir) - 1);
    }

    char line[2048];
    long total_written = 0;

    while (fgets(line, sizeof(line), m3u8) != NULL) {
        if (atomic_load(&g_cancelled) != 0) {
            fclose(output);
            fclose(m3u8);
            remove(output_path);
            return -4;
        }

        trim_line(line);
        if (line[0] == '\0' || line[0] == '#') {
            continue;
        }

        char resolved_path[PATH_MAX];
        memset(resolved_path, 0, sizeof(resolved_path));

        if (is_uri_or_abs_path(line)) {
            if (strstr(line, "://") != NULL) {
                continue;
            }
            strncpy(resolved_path, line, sizeof(resolved_path) - 1);
        } else {
            snprintf(resolved_path, sizeof(resolved_path), "%s/%s", base_dir, line);
        }

        if (copy_file_to_stream(resolved_path, output, &total_written) != 0) {
            fclose(output);
            fclose(m3u8);
            remove(output_path);
            return -3;
        }
    }

    fflush(output);
    fclose(output);
    fclose(m3u8);

    if (total_written <= 0) {
        remove(output_path);
        return -3;
    }

    return 0;
}

static void log_ffmpeg_error(const char *prefix, int errnum) {
    char errbuf[AV_ERROR_MAX_STRING_SIZE];
    memset(errbuf, 0, sizeof(errbuf));
    av_strerror(errnum, errbuf, sizeof(errbuf));
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%s: (%d) %s", prefix, errnum, errbuf);
}

static const char *media_type_name(enum AVMediaType media_type) {
    switch (media_type) {
        case AVMEDIA_TYPE_VIDEO:
            return "video";
        case AVMEDIA_TYPE_AUDIO:
            return "audio";
        case AVMEDIA_TYPE_SUBTITLE:
            return "subtitle";
        case AVMEDIA_TYPE_DATA:
            return "data";
        case AVMEDIA_TYPE_ATTACHMENT:
            return "attachment";
        default:
            return "unknown";
    }
}

static void log_stream_info(AVFormatContext *ifmt_ctx) {
    if (ifmt_ctx == NULL) {
        return;
    }
    __android_log_print(ANDROID_LOG_INFO, TAG, "input stream count=%u, duration=%" PRId64, ifmt_ctx->nb_streams, ifmt_ctx->duration);
    for (unsigned int i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *stream = ifmt_ctx->streams[i];
        if (stream == NULL || stream->codecpar == NULL) {
            continue;
        }
        __android_log_print(
                ANDROID_LOG_INFO,
                TAG,
                "stream idx=%u type=%s codec=%s codec_id=%d width=%d height=%d sample_rate=%d channels=%d",
                i,
                media_type_name(stream->codecpar->codec_type),
                avcodec_get_name(stream->codecpar->codec_id),
                stream->codecpar->codec_id,
                stream->codecpar->width,
                stream->codecpar->height,
                stream->codecpar->sample_rate,
                stream->codecpar->ch_layout.nb_channels
        );
    }
}

static int is_mp4_codec_supported(enum AVMediaType media_type, enum AVCodecID codec_id) {
    if (media_type == AVMEDIA_TYPE_VIDEO) {
        return codec_id == AV_CODEC_ID_H264 || codec_id == AV_CODEC_ID_HEVC || codec_id == AV_CODEC_ID_MPEG4;
    }
    if (media_type == AVMEDIA_TYPE_AUDIO) {
        return codec_id == AV_CODEC_ID_AAC || codec_id == AV_CODEC_ID_MP3 || codec_id == AV_CODEC_ID_AC3 || codec_id == AV_CODEC_ID_EAC3;
    }
    return 0;
}

static int remux_with_ffmpeg(const char *input_path, const char *output_path, const InputOpenOptions *options) {


    AVFormatContext *ifmt_ctx = NULL;
    AVFormatContext *ofmt_ctx = NULL;
    AVDictionary *input_opts = NULL;
    int ret = 0;

    const char *protocol_whitelist = "file,crypto,data,http,https,tcp,tls,udp,rtp,rtmp,rtsp";

    const char *allowed_extensions = "ALL";
    char *normalized_headers = NULL;

    if (options != NULL) {
        if (options->protocol_whitelist != NULL && options->protocol_whitelist[0] != '\0') {
            protocol_whitelist = options->protocol_whitelist;
        }
        if (options->allowed_extensions != NULL && options->allowed_extensions[0] != '\0') {
            allowed_extensions = options->allowed_extensions;
        }
        normalized_headers = normalize_headers(options->headers);
    }

    av_dict_set(&input_opts, "protocol_whitelist", protocol_whitelist, 0);
    av_dict_set(&input_opts, "allowed_extensions", allowed_extensions, 0);
    if (normalized_headers != NULL) {
        av_dict_set(&input_opts, "headers", normalized_headers, 0);
    }
    if (options != NULL && options->cookies != NULL && options->cookies[0] != '\0') {
        av_dict_set(&input_opts, "cookies", options->cookies, 0);
    }
    if (options != NULL && options->user_agent != NULL && options->user_agent[0] != '\0') {
        av_dict_set(&input_opts, "user_agent", options->user_agent, 0);
    }
    if (options != NULL && options->referer != NULL && options->referer[0] != '\0') {
        av_dict_set(&input_opts, "referer", options->referer, 0);
    }

    ret = avformat_open_input(&ifmt_ctx, input_path, NULL, &input_opts);
    av_dict_free(&input_opts);
    free(normalized_headers);

    if (ret < 0) {
        log_ffmpeg_error("avformat_open_input failed", ret);
        goto end;
    }

    ret = avformat_find_stream_info(ifmt_ctx, NULL);
    if (ret < 0) {
        log_ffmpeg_error("avformat_find_stream_info failed", ret);
        goto end;
    }
    log_stream_info(ifmt_ctx);

    ret = avformat_alloc_output_context2(&ofmt_ctx, NULL, "mp4", output_path);
    if (ret < 0 || ofmt_ctx == NULL) {
        if (ret < 0) {
            log_ffmpeg_error("avformat_alloc_output_context2 failed", ret);
        }
        ret = AVERROR_UNKNOWN;
        goto end;
    }

    int stream_mapping_size = (int) ifmt_ctx->nb_streams;
    int *stream_mapping = av_calloc((size_t) stream_mapping_size, sizeof(int));
    if (stream_mapping == NULL) {
        ret = AVERROR(ENOMEM);
        goto end;
    }
    for (int i = 0; i < stream_mapping_size; i++) {
        stream_mapping[i] = -1;
    }

    int out_stream_index = 0;
    for (unsigned int i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *in_stream = ifmt_ctx->streams[i];
        enum AVMediaType media_type = in_stream->codecpar->codec_type;
        enum AVCodecID codec_id = in_stream->codecpar->codec_id;

        if (!is_mp4_codec_supported(media_type, codec_id)) {
            __android_log_print(ANDROID_LOG_WARN, TAG, "skip stream idx=%u type=%d codec=%s", i, media_type, avcodec_get_name(codec_id));
            continue;
        }

        AVStream *out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (out_stream == NULL) {
            av_free(stream_mapping);
            ret = AVERROR(ENOMEM);
            goto end;
        }

        ret = avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
        if (ret < 0) {
            av_free(stream_mapping);
            log_ffmpeg_error("avcodec_parameters_copy failed", ret);
            goto end;
        }

        out_stream->codecpar->codec_tag = 0;
        out_stream->time_base = in_stream->time_base;
        stream_mapping[i] = out_stream_index++;
    }

    if (out_stream_index <= 0) {
        av_free(stream_mapping);
        ret = AVERROR(EINVAL);
        __android_log_print(ANDROID_LOG_ERROR, TAG, "no supported stream for mp4");
        goto end;
    }

    if (!(ofmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, output_path, AVIO_FLAG_WRITE);
        if (ret < 0) {
            av_free(stream_mapping);
            log_ffmpeg_error("avio_open failed", ret);
            goto end;
        }
    }

    ret = avformat_write_header(ofmt_ctx, NULL);
    if (ret < 0) {
        av_free(stream_mapping);
        log_ffmpeg_error("avformat_write_header failed", ret);
        goto end;
    }

    int invalid_packet_count = 0;
    int read_error_count = 0;
    int corrupt_packet_count = 0;
    while (1) {
        AVPacket pkt = {0};

        if (atomic_load(&g_cancelled) != 0) {
            ret = AVERROR_EXIT;
            av_packet_unref(&pkt);
            break;
        }

        ret = av_read_frame(ifmt_ctx, &pkt);
        if (ret < 0) {
            av_packet_unref(&pkt);
            if (ret == AVERROR_EOF) {
                ret = 0;
            } else if ((ret == AVERROR_INVALIDDATA || ret == AVERROR(EIO)) && read_error_count < 32) {
                read_error_count++;
                log_ffmpeg_error("av_read_frame recoverable error, skip fragment", ret);
                continue;
            }
            break;
        }
        read_error_count = 0;

        if (pkt.stream_index < 0 || pkt.stream_index >= stream_mapping_size || stream_mapping[pkt.stream_index] < 0) {
            av_packet_unref(&pkt);
            continue;
        }

        if ((pkt.flags & AV_PKT_FLAG_CORRUPT) != 0) {
            if (corrupt_packet_count < 128) {
                corrupt_packet_count++;
                __android_log_print(
                        ANDROID_LOG_WARN,
                        TAG,
                        "skip corrupt packet stream=%d pts=%" PRId64 " dts=%" PRId64 " size=%d",
                        pkt.stream_index,
                        pkt.pts,
                        pkt.dts,
                        pkt.size
                );
            }
            av_packet_unref(&pkt);
            continue;
        }

        AVStream *in_stream = ifmt_ctx->streams[pkt.stream_index];
        pkt.stream_index = stream_mapping[pkt.stream_index];
        AVStream *out_stream = ofmt_ctx->streams[pkt.stream_index];

        av_packet_rescale_ts(&pkt, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;

        int write_ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
        av_packet_unref(&pkt);
        if (write_ret < 0) {
            if (write_ret == AVERROR(EINVAL) && invalid_packet_count < 1024) {
                invalid_packet_count++;
                __android_log_print(ANDROID_LOG_WARN, TAG, "skip invalid packet write, count=%d", invalid_packet_count);
                continue;
            }
            ret = write_ret;
            log_ffmpeg_error("av_interleaved_write_frame failed", ret);
            break;
        }
    }

    av_free(stream_mapping);



    if (ret == 0) {
        ret = av_write_trailer(ofmt_ctx);
        if (ret < 0) {
            log_ffmpeg_error("av_write_trailer failed", ret);
        }
    }

end:
    if (ofmt_ctx != NULL) {
        if (!(ofmt_ctx->oformat->flags & AVFMT_NOFILE) && ofmt_ctx->pb != NULL) {
            avio_closep(&ofmt_ctx->pb);
        }
        avformat_free_context(ofmt_ctx);
    }

    if (ifmt_ctx != NULL) {
        avformat_close_input(&ifmt_ctx);
    }

    if (ret < 0) {
        remove(output_path);
    }

    return ret;
}

static char *get_jstring_copy(JNIEnv *env, jstring jstr) {
    if (jstr == NULL) {
        return NULL;
    }
    const char *raw = (*env)->GetStringUTFChars(env, jstr, 0);
    if (raw == NULL) {
        return NULL;
    }
    char *copy = strdup(raw);
    (*env)->ReleaseStringUTFChars(env, jstr, raw);
    return copy;
}

JNIEXPORT jint JNICALL
Java_com_orange_ffmpeg_FFmpegKit_nativeInit(JNIEnv *env, jclass clazz) {
    (void) env;
    (void) clazz;
    atomic_store(&g_cancelled, 0);
    av_log_set_level(AV_LOG_ERROR);
    return avformat_network_init();
}

JNIEXPORT jint JNICALL
Java_com_orange_ffmpeg_FFmpegKit_nativeExecute(JNIEnv *env, jclass clazz, jobjectArray args) {
    (void) clazz;

    if (atomic_load(&g_cancelled) != 0) {
        return -4;
    }
    if (args == NULL) {
        return -3;
    }

    char *input_path = NULL;
    char *output_path = NULL;
    InputOpenOptions input_options;
    memset(&input_options, 0, sizeof(input_options));

    jsize len = (*env)->GetArrayLength(env, args);
    for (jsize i = 0; i < len; i++) {
        if (atomic_load(&g_cancelled) != 0) {
            free(input_path);
            free(output_path);
            free_input_open_options(&input_options);
            return -4;
        }

        jstring arg_obj = (jstring) (*env)->GetObjectArrayElement(env, args, i);
        char *arg = get_jstring_copy(env, arg_obj);
        (*env)->DeleteLocalRef(env, arg_obj);

        if (arg == NULL) {
            continue;
        }

        if (strcmp(arg, "-version") == 0) {
            free(arg);
            free(input_path);
            free(output_path);
            free_input_open_options(&input_options);
            return 0;
        }

        if (i + 1 < len && (
                strcmp(arg, "-i") == 0 ||
                strcmp(arg, "-headers") == 0 ||
                strcmp(arg, "-cookies") == 0 ||
                strcmp(arg, "-cookie") == 0 ||
                strcmp(arg, "-user_agent") == 0 ||
                strcmp(arg, "-user-agent") == 0 ||
                strcmp(arg, "-referer") == 0 ||
                strcmp(arg, "-protocol_whitelist") == 0 ||
                strcmp(arg, "-allowed_extensions") == 0
        )) {
            jstring next_obj = (jstring) (*env)->GetObjectArrayElement(env, args, i + 1);
            char *next = get_jstring_copy(env, next_obj);
            (*env)->DeleteLocalRef(env, next_obj);

            if (next != NULL) {
                if (strcmp(arg, "-i") == 0) {
                    free(input_path);
                    input_path = next;
                } else if (strcmp(arg, "-headers") == 0) {
                    free(input_options.headers);
                    input_options.headers = next;
                } else if (strcmp(arg, "-cookies") == 0 || strcmp(arg, "-cookie") == 0) {
                    free(input_options.cookies);
                    input_options.cookies = next;
                } else if (strcmp(arg, "-user_agent") == 0 || strcmp(arg, "-user-agent") == 0) {
                    free(input_options.user_agent);
                    input_options.user_agent = next;
                } else if (strcmp(arg, "-referer") == 0) {
                    free(input_options.referer);
                    input_options.referer = next;
                } else if (strcmp(arg, "-protocol_whitelist") == 0) {
                    free(input_options.protocol_whitelist);
                    input_options.protocol_whitelist = next;
                } else if (strcmp(arg, "-allowed_extensions") == 0) {
                    free(input_options.allowed_extensions);
                    input_options.allowed_extensions = next;
                } else {
                    free(next);
                }
            }
            i++;
            free(arg);
            continue;
        }

        if (arg[0] != '-') {
            free(output_path);
            output_path = strdup(arg);
        }

        free(arg);
    }


    if (input_path == NULL || output_path == NULL) {
        free(input_path);
        free(output_path);
        free_input_open_options(&input_options);
        return -3;
    }

    int ff_ret = remux_with_ffmpeg(input_path, output_path, &input_options);
    int ret;
    if (ff_ret == 0) {
        ret = 0;
    } else if (ff_ret == AVERROR_EXIT) {
        ret = -4;
    } else {
        __android_log_print(ANDROID_LOG_WARN, TAG, "remux_with_ffmpeg failed: %d", ff_ret);
        ret = -2;
    }


    free(input_path);
    free(output_path);
    free_input_open_options(&input_options);
    return ret;

}

JNIEXPORT void JNICALL
Java_com_orange_ffmpeg_FFmpegKit_nativeCancel(JNIEnv *env, jclass clazz) {
    (void) env;
    (void) clazz;
    atomic_store(&g_cancelled, 1);
}

JNIEXPORT jstring JNICALL
Java_com_orange_ffmpeg_FFmpegKit_nativeGetVersion(JNIEnv *env, jclass clazz) {
    (void) clazz;
    const char *version = av_version_info();
    if (version == NULL || version[0] == '\0') {
        version = "unknown";
    }
    return (*env)->NewStringUTF(env, version);
}
