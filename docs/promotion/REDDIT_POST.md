# Reddit Post Template

## Title
[Open Source] OrangePlayer - Feature-rich Android video player library with danmaku, subtitles, OCR, and speech recognition

## Post Content

Hey r/androiddev! 👋

I've been working on **OrangePlayer**, an enhanced Android video player library that I'd like to share with the community.

### 🎯 What is it?

OrangePlayer is built on top of GSYVideoPlayer and adds a bunch of useful features that are commonly needed but tedious to implement:

- 🎬 **Multiple playback engines** (System/ExoPlayer/IJK/AliPlayer) with runtime switching
- 📝 **Subtitle system** supporting SRT/ASS/VTT formats
- 🔤 **OCR subtitle recognition** using Tesseract + ML Kit translation
- 🎤 **Speech recognition** with Vosk for real-time subtitle generation
- 💬 **Danmaku (bullet comments)** with customizable speed/size/opacity
- 🎛️ **Variable playback speed** (0.35x - 10x)
- 📺 **DLNA casting** support
- 🖼️ **Picture-in-Picture** mode
- And more...

### 📦 Installation

```gradle
dependencies {
    implementation 'com.github.706412584:orangeplayer:v1.0.5'
    implementation 'io.github.706412584:gsyVideoPlayer-java:1.1.0'
    implementation 'io.github.706412584:gsyVideoPlayer-exo_player2:1.1.0'
}
```

### 🚀 Basic Usage

```java
OrangevideoView videoView = findViewById(R.id.video_player);
videoView.setUp("https://example.com/video.mp4", true, "Video Title");
videoView.startPlayLogic();
```

That's it! All UI components are automatically created and configured.

### 🌟 Why OrangePlayer?

**vs GSYVideoPlayer**: Adds danmaku, subtitles, OCR, speech recognition, and casting out of the box

**vs ExoPlayer**: Provides complete UI components and additional features

**vs IJKPlayer**: Supports multiple engines with easy switching

### 📚 Documentation

- [GitHub Repository](https://github.com/706412584/orangeplayer)
- [Installation Guide](https://github.com/706412584/orangeplayer/blob/main/docs/INSTALLATION.md)
- [API Documentation](https://github.com/706412584/orangeplayer/blob/main/docs/API.md)
- [15 Complete Examples](https://github.com/706412584/orangeplayer/blob/main/docs/EXAMPLES.md)

### 🎥 Demo

[Download Demo APK](https://github.com/706412584/orangeplayer/releases/tag/demo)

### 🤝 Contributing

Contributions are welcome! Feel free to:
- Report bugs
- Suggest features
- Submit pull requests
- Improve documentation

### 📄 License

Apache License 2.0

---

I'd love to hear your feedback! What features would you like to see added?

**GitHub**: https://github.com/706412584/orangeplayer

If you find it useful, a ⭐️ would be much appreciated!

---

## Suggested Subreddits

1. **r/androiddev** (Primary target)
   - Best time to post: Tuesday-Thursday, 9-11 AM EST
   - Flair: Library

2. **r/opensource**
   - Best time: Any weekday, 8-10 AM EST
   - Flair: Project

3. **r/programming**
   - Best time: Monday-Wednesday, 9 AM EST
   - Flair: Project

4. **r/Android**
   - Best time: Weekend, 10 AM EST
   - Flair: Dev

## Engagement Tips

1. **Respond quickly** to comments (within 1-2 hours)
2. **Be helpful** - answer questions thoroughly
3. **Accept criticism** gracefully
4. **Provide examples** when asked
5. **Update the post** with common Q&A

## Common Questions to Prepare For

**Q: How is this different from GSYVideoPlayer?**
A: OrangePlayer is built on GSYVideoPlayer but adds many features out of the box: danmaku, subtitles, OCR recognition, speech recognition, and DLNA casting. It saves you from having to integrate these separately.

**Q: What's the APK size impact?**
A: Core library is ~500KB. Optional features (OCR, speech recognition) add 2-5MB each. You only include what you need.

**Q: Does it support Jetpack Compose?**
A: Currently it's View-based, but you can use AndroidView to integrate it into Compose. Native Compose support is planned for future releases.

**Q: What about performance?**
A: Performance is comparable to GSYVideoPlayer since it's built on top of it. OCR and speech recognition run on background threads to avoid blocking the UI.

**Q: Is it production-ready?**
A: Yes, it's being used in production apps. However, as with any library, thorough testing in your specific use case is recommended.
