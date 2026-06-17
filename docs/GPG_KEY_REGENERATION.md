# GPG 密钥重新生成指南

## 步骤 1：删除旧密钥（可选）

如果要删除旧密钥：

```bash
# 查看现有密钥
gpg --list-keys

# 删除公钥
gpg --delete-key C60913C4

# 删除私钥
gpg --delete-secret-key C60913C4
```

## 步骤 2：生成新的 GPG 密钥

### 方法一：交互式生成（推荐）

```bash
# 启动密钥生成向导
gpg --full-generate-key
```

然后按照提示操作：

1. **选择密钥类型**
   ```
   Please select what kind of key you want:
      (1) RSA and RSA (default)
      (2) DSA and Elgamal
      (3) DSA (sign only)
      (4) RSA (sign only)
   Your selection? 1
   ```
   **输入：1** （选择 RSA and RSA）

2. **选择密钥长度**
   ```
   RSA keys may be between 1024 and 4096 bits long.
   What keysize do you want? (3072) 4096
   ```
   **输入：4096** （使用最强加密）

3. **设置有效期**
   ```
   Please specify how long the key should be valid.
            0 = key does not expire
         <n>  = key expires in n days
         <n>w = key expires in n weeks
         <n>m = key expires in n months
         <n>y = key expires in n years
   Key is valid for? (0) 0
   ```
   **输入：0** （永不过期）
   
   确认：
   ```
   Key does not expire at all
   Is this correct? (y/N) y
   ```
   **输入：y**

4. **输入用户信息**
   ```
   Real name: xcwl
   Email address: 706412584@qq.com
   Comment: Maven Central Publishing Key
   ```
   
   确认：
   ```
   You selected this USER-ID:
       "xcwl (Maven Central Publishing Key) <706412584@qq.com>"
   
   Change (N)ame, (C)omment, (E)mail or (O)kay/(Q)uit? O
   ```
   **输入：O**

5. **设置密码**
   - 会弹出对话框要求输入密码
   - **请记住这个密码**，后面会用到
   - 建议使用强密码（至少 12 位，包含大小写字母、数字、符号）

6. **等待生成**
   - 系统会生成密钥，可能需要几分钟
   - 可以移动鼠标、敲击键盘来增加随机性

### 方法二：命令行生成（快速）

```bash
gpg --batch --gen-key <<EOF
Key-Type: RSA
Key-Length: 4096
Subkey-Type: RSA
Subkey-Length: 4096
Name-Real: xcwl
Name-Email: 706412584@qq.com
Name-Comment: Maven Central Publishing Key
Expire-Date: 0
Passphrase: 你的密码
%commit
EOF
```

## 步骤 3：查看新密钥

```bash
# 查看所有密钥
gpg --list-keys

# 输出示例：
# pub   rsa4096 2026-01-08 [SC]
#       ABCD1234EFGH5678IJKL9012MNOP3456QRST7890
# uid           [ultimate] xcwl (Maven Central Publishing Key) <706412584@qq.com>
# sub   rsa4096 2026-01-08 [E]
```

**记录密钥 ID**：取最后 8 位，例如 `QRST7890`

## 步骤 4：导出密钥

### 导出私钥

```bash
# 导出到文件
gpg --export-secret-keys -o C:\Users\70641\secring-new.gpg

# 或者导出为 ASCII 格式（推荐用于 CI/CD）
gpg --armor --export-secret-keys > C:\Users\70641\secring-new.asc
```

### 导出公钥

```bash
# 导出公钥（用于验证）
gpg --armor --export 你的密钥ID > C:\Users\70641\pubkey.asc
```

## 步骤 5：上传公钥到密钥服务器

**重要：必须上传到多个服务器以确保可用性**

```bash
# 替换 YOUR_KEY_ID 为你的密钥 ID（最后 8 位）
set KEY_ID=YOUR_KEY_ID

# 上传到 Ubuntu 密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys %KEY_ID%

# 上传到 OpenPGP 密钥服务器
gpg --keyserver keys.openpgp.org --send-keys %KEY_ID%

# 上传到 MIT 密钥服务器
gpg --keyserver pgp.mit.edu --send-keys %KEY_ID%
```

### 验证上传成功

等待 5-10 分钟后验证：

```bash
# 从服务器接收密钥（测试）
gpg --keyserver keyserver.ubuntu.com --recv-keys %KEY_ID%
```

如果成功，会显示：
```
gpg: key YOUR_KEY_ID: "xcwl (Maven Central Publishing Key) <706412584@qq.com>" not changed
gpg: Total number processed: 1
gpg:              unchanged: 1
```

## 步骤 6：更新 gradle.properties

编辑 `gradle.properties` 文件：

```properties
# Maven Central 发布配置
mavenCentralUsername=你的用户名
mavenCentralPassword=你的密码

# GPG 签名配置（使用新密钥）
signing.keyId=新密钥的最后8位
signing.password=你设置的GPG密码
signing.secretKeyRingFile=C:/Users/70641/secring-new.gpg
```

## 步骤 7：测试签名

```bash
# 清理并重新发布
.\gradlew clean :palyerlibrary:publishToMavenLocal

# 检查签名文件
dir palyerlibrary\build\libs\*.asc
```

## 步骤 8：发布到 Maven Central

```bash
# 更新版本号到 1.1.0（新密钥需要新版本）
# 编辑 palyerlibrary/build.gradle，修改版本号

# 发布
.\gradlew clean :palyerlibrary:publishToMavenCentral
```

## 常见问题

### Q1: 生成密钥时卡住不动？

**A:** 系统需要收集随机数据。解决方法：
- 移动鼠标
- 敲击键盘
- 打开浏览器浏览网页
- 复制大文件

### Q2: 找不到 gpg 命令？

**A:** 需要安装 GPG：
- Windows: 下载 [Gpg4win](https://www.gpg4win.org/)
- 安装后，GPG 路径通常在：`C:\Program Files (x86)\GnuPG\bin\gpg.exe`
- 添加到 PATH 环境变量

### Q3: 密钥上传失败？

**A:** 尝试：
1. 检查网络连接
2. 使用代理（如果在国内）
3. 尝试不同的密钥服务器
4. 等待几分钟后重试

### Q4: Maven Central 仍然验证失败？

**A:** 可能原因：
1. 公钥还未同步到所有服务器（等待 1-2 小时）
2. 密钥 ID 配置错误（检查 gradle.properties）
3. 密码错误（重新输入）
4. 密钥文件路径错误（使用绝对路径）

## 完整示例

假设生成的密钥 ID 是 `ABCD1234`，密码是 `MySecurePassword123!`：

**gradle.properties:**
```properties
mavenCentralUsername=abc123def
mavenCentralPassword=xyz789uvw

signing.keyId=ABCD1234
signing.password=MySecurePassword123!
signing.secretKeyRingFile=C:/Users/70641/secring-new.gpg
```

**发布命令:**
```bash
.\gradlew clean :palyerlibrary:publishToMavenCentral
```

## 下一步

生成新密钥后：
1. ✅ 更新 gradle.properties
2. ✅ 上传公钥到密钥服务器
3. ✅ 等待 10-30 分钟让密钥同步
4. ✅ 更新版本号到 1.1.0
5. ✅ 重新发布到 Maven Central
6. ✅ 检查验证状态

如果还是失败，建议使用 JitPack 作为替代方案。
