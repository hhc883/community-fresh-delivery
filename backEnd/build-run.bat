@echo off
chcp 65001 >nul
REM ============================================================
REM  苍穹外卖 —— 构建 / 启动 / 打包
REM
REM  JDK 说明：
REM    项目原用 Lombok 1.18.20，只支持到 JDK 16；IDEA 编译进程跑在
REM    JDK 21 上会抛 NoSuchFieldError 并崩溃。已把 Lombok 升级到
REM    1.18.34（兼容 JDK 8~21），因此现在可直接用 JDK 21 编译与运行。
REM    原先使用的 Temurin JDK 8 目录在本机被外部进程长期占用
REM    （CreateProcess error=32），故改用已验证可用的 JDK 21。
REM    编译产物仍是 Java 8 字节码（pom 中 source/target=1.8）。
REM
REM  用法：双击运行。可带参数，例如：
REM    构建.bat                  -> 编译（clean compile）
REM    构建.bat clean package    -> 打包
REM    构建.bat run              -> 编译并启动后端（端口 8080）
REM    构建.bat package run      -> 打包并启动
REM ============================================================

set "MVN_HOME=%USERPROFILE%\.dsh-tools\apache-maven-3.9.9"
set "JAVA_HOME=C:\Users\Administrator\.jdks\dragonwell-ex-21.0.12"
set "CW=%MVN_HOME%\boot\plexus-classworlds-2.8.0.jar"
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "JAR=sky-server\target\sky-server-1.0-SNAPSHOT.jar"

cd /d "%~dp0"

if not exist "%JAVA%" (
    echo [错误] 未找到 JDK: %JAVA%
    echo        请确认 JDK 21 已安装在上述路径。
    pause & exit /b 1
)
if not exist "%CW%" (
    echo [错误] 未找到 Maven: %MVN_HOME%
    pause & exit /b 1
)

REM ---------- run：打包并启动后端 ----------
if /i "%~1"=="run" (
    setlocal enabledelayedexpansion
    set "STEPS=%*"
    set "STEPS=!STEPS:run=!"
    if not "!STEPS!"=="" (
        echo === 先执行: mvn !STEPS! -DskipTests ===
        "%JAVA%" -classpath "%CW%" "-Dclassworlds.conf=%MVN_HOME%\bin\m2.conf" "-Dmaven.home=%MVN_HOME%" "-Dmaven.multiModuleProjectDirectory=%CD%" org.codehaus.plexus.classworlds.launcher.Launcher !STEPS! -DskipTests
        if errorlevel 1 (
            echo [失败] 构建未通过，跳过启动。
            pause & exit /b 1
        )
    )

    REM 端口占用检查
    netstat -ano | findstr ":8080 " | findstr "LISTENING" >nul
    if not errorlevel 1 (
        echo.
        echo [错误] 8080 端口已被占用，请先停止占用该端口的程序。
        for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do echo   PID: %%p
        pause & exit /b 1
    )

    if not exist "%JAR%" (
        echo.
        echo [错误] 未找到 %JAR%
        echo        请先打包：构建.bat clean package -DskipTests
        pause & exit /b 1
    )

    echo.
    echo ============================================================
    echo   启动后端 (Ctrl+C 停止)
    echo   接口文档: http://localhost:8080/doc.html
    echo ============================================================
    echo.
    "%JAVA%" -Dfile.encoding=UTF-8 -jar "%JAR%"
    pause
    exit /b 0
)

REM ---------- 默认：Maven 构建 ----------
if "%~1"=="" (
    set "ARGS=clean compile"
) else (
    set "ARGS=%*"
)

echo ============================================================
echo  JDK : %JAVA_HOME%
echo  MVN : %MVN_HOME%
echo  命令: mvn %ARGS%
echo ============================================================
echo.

"%JAVA%" -classpath "%CW%" "-Dclassworlds.conf=%MVN_HOME%\bin\m2.conf" "-Dmaven.home=%MVN_HOME%" "-Dmaven.multiModuleProjectDirectory=%CD%" org.codehaus.plexus.classworlds.launcher.Launcher %ARGS%

if errorlevel 1 (
    echo.
    echo [失败] 构建未通过，请把上方 ERROR 行发给助手。
) else (
    echo.
    echo [成功] 构建通过。
    echo.
    echo 下一步：
    echo   构建.bat run           启动后端（端口 8080）
    echo   或双击 verify-groupon.http 测试接口（需 IDE 的 HTTP Client）
)
pause

