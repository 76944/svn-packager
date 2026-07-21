# SVN增量打包工具 - 操作说明

## 一、完整打包流程

```
1. 配置SVN项目
   ↓
2. 获取SVN提交日志
   ↓
3. 选择要打包的提交记录
   ↓
4. 执行增量打包
   ↓
5. 获得ZIP文件，部署到Tomcat
```

---

## 二、详细步骤

### 步骤1：配置SVN项目

1. 启动程序
2. 点击"新增"按钮
3. 填写项目信息：
   - **项目名称**：自定义名称（如：myapp）
   - **SVN地址**：SVN仓库URL
   - **本地路径**：项目本地目录（如：D:\workspace\myapp）
   - **SVN用户名/密码**：如有认证则填写
4. 点击"测试连接"验证SVN是否可访问
5. 保存配置

### 步骤2：获取提交日志

1. 在左侧项目列表中选择项目
2. 设置日期范围（开始日期 ~ 结束日期）
3. 点击"获取日志"
4. 等待加载完成，显示提交记录列表

### 步骤3：选择提交记录

1. 在日志表格中勾选需要打包的提交记录
   - 支持多选（Ctrl+点击）
   - 支持范围选择（Shift+点击）
2. 可使用搜索框按作者或提交说明过滤

### 步骤4：执行增量打包

1. 点击"打包"按钮
2. 选择输出目录
3. 等待打包完成
4. 获得ZIP文件（格式：`项目名_incremental_时间戳.zip`）

### 步骤5：部署到Tomcat

1. 将ZIP文件解压到Tomcat的 `webapps` 目录
2. 重启Tomcat服务
3. 验证应用是否正常运行

---

## 三、打包前必须做的事（重要）

### 必须执行 Build Artifacts

**工具只能读取 Build Artifacts 生成的文件，不能读取 Ctrl+F9 编译的文件。**

| 操作 | 输出位置 | 工具能读取？ |
|------|---------|------------|
| Ctrl+F9（编译） | `out/production/<module>/` | ❌ 不能 |
| Build Artifacts（构建） | `out/artifacts/<artifact>/` | ✅ 能 |

**正确操作顺序：**

```
修改代码 → Build Artifacts → 打开工具 → 选择提交记录 → 执行增量打包
```

**如何执行 Build Artifacts：**

- IDEA菜单：Build → Build Artifacts → 选择你的Artifact → 点击 Build
- 或使用快捷键（如有配置）

---

## 四、工具查找编译产物的逻辑

工具按以下顺序搜索目录（优先级从高到低）：

```
1. 项目目录/target/           ← Maven构建产物
2. 项目目录/out/artifacts/    ← IDEA Build Artifacts
3. 项目目录/out/              ← 递归搜索3层
4. 项目目录/build/            ← Gradle产物
```

**要求：目录内必须包含 `WEB-INF` 子目录**

---

## 五、路径映射规则

SVN路径会自动映射到WAR目录中的对应文件：

| SVN路径 | WAR输出路径 |
|---------|-----------|
| `trunk/src/main/java/**/*.java` | `WEB-INF/classes/**/*.class` |
| `trunk/src/main/resources/**` | `WEB-INF/classes/**` |
| `trunk/src/main/webapp/**` | WAR根目录 |
| `WebRoot/**` | WAR根目录 |

### 分支/标签路径

- `branches/dev/src/main/java/...` → 同样映射到 `WEB-INF/classes/`
- `tags/v1.0/src/main/java/...` → 同样映射到 `WEB-INF/classes/`

---

## 六、排除文件列表

以下配置文件在增量打包时会被自动跳过：

```
license.xml
web.xml
config.properties
db.properties
email.properties
logback.xml
redis.properties
wechat-config.properties
whitelist.xml
sn.txt
```

---

## 七、常见问题

### Q: 提示"未找到编译产物（war展开目录），请先编译项目"

**原因**：工具找不到包含 `WEB-INF` 的目录。

**解决**：
1. 确认已执行 Build Artifacts（不是只按 Ctrl+F9）
2. 检查项目目录下是否存在 `out/artifacts/` 目录
3. 如果是 Maven 项目，确认已执行 `mvn clean package`

### Q: 打包的文件在Tomcat中无法访问

**检查**：
1. 确认ZIP文件解压后的目录结构包含 `WEB-INF/classes/`
2. 确认没有遗漏需要的配置文件（排除列表中的文件需要手动添加）

### Q: 打包的不是最新代码

**原因**：只执行了 Ctrl+F9，没有执行 Build Artifacts。

**解决**：每次打包前先执行 Build Artifacts。

### Q: SVN连接失败

**检查**：
1. 网络连接是否正常
2. SVN地址是否正确
3. 用户名密码是否正确
4. SVN服务器是否允许HTTP/HTTPS访问

### Q: Maven编译失败

**检查**：
1. 系统PATH中是否包含Maven
2. 项目根目录是否有pom.xml
3. Java版本是否符合要求（JDK 1.8+）

---

## 八、配置文件位置

- **Windows**: `%APPDATA%\SVNPackager\config\config.json`
- **Linux/Mac**: `~/.svn-packager/config.json`

---

## 九、打包输出说明

### 文件命名规则

- **全量打包**: `项目名_时间戳.zip`
- **增量打包**: `项目名_incremental_时间戳.zip`

### ZIP文件结构（示例）

```
WEB-INF/
├── classes/
│   └── com/
│       └── example/
│           └── MyClass.class
├── lib/
│   └── dependencies.jar
└── web.xml
index.jsp
static/
└── css/
    └── style.css
```

---

## 十、系统要求

- Windows 7/8/10/11（64位推荐）
- Java运行时环境 JDK/JRE 1.8 或更高版本
- Maven（编译项目时需要）
- SVN命令行工具（可选，工具使用SVNKit库）
