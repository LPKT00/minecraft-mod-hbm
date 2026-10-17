@echo off
chcp 65001 >nul
title HBM Explorer's Compass - Автоматична збірка (1.12.2)
echo =====================================================================
echo      HBM Explorer's Compass Mod (1.12.2) - Автоматичний збирач
echo =====================================================================
echo.

cd /d "%~dp0"

if not exist "libs" mkdir "libs"

REM 1. Пошук Java 8 (1.8), яка необхідна для Forge 1.12.2
echo [1/3] Перевірка наявності Java 8 (1.8)...

set "JAVA8_FOUND=0"
java -version 2>&1 | findstr /i "1.8" >nul
if %errorlevel%==0 (
    set "JAVA8_FOUND=1"
    echo [OK] Системна Java 8 знайдена!
)

if "%JAVA8_FOUND%"=="0" (
    echo Системна Java не є версією 1.8. Шукаємо встановлену Java 8 на комп'ютері...
    for /d %%i in ("C:\Program Files\Java\jdk1.8*" "C:\Program Files\Java\jre1.8*" "C:\Program Files\Eclipse Adoptium\jdk-8*" "C:\Program Files\BellSoft\LibericaJDK-8*" "C:\Program Files (x86)\Minecraft Launcher\runtime\jre-legacy\windows-x64\jre-legacy" "%APPDATA%\.minecraft\runtime\jre-legacy\windows-x64\jre-legacy") do (
        if exist "%%~i\bin\javac.exe" (
            set "JAVA_HOME=%%~i"
            set "PATH=%%~i\bin;%PATH%"
            set "JAVA8_FOUND=1"
            echo [OK] Знайдено JDK 8: %%~i
            goto :java_ready
        )
    )
)

:java_ready
if "%JAVA8_FOUND%"=="0" (
    echo [УВАГА] JDK 8 (Java Development Kit 1.8) не знайдено у стандартних папках.
    echo Завантажуємо портативну Java 8 (JDK 8) автоматично у папку проєкту...
    if not exist ".gradle_tools\jdk8\bin\javac.exe" (
        mkdir ".gradle_tools" 2>nul
        echo Завантаження OpenJDK 8 (~100 МБ)... Зачекайте, будь ласка...
        powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u402-b06/OpenJDK8U-jdk_x64_windows_hotspot_8u402b06.zip' -OutFile '.gradle_tools\jdk8.zip'"
        if exist ".gradle_tools\jdk8.zip" (
            echo Розпакування портативної Java 8...
            powershell -Command "Expand-Archive -Path '.gradle_tools\jdk8.zip' -DestinationPath '.gradle_tools\jdk8_temp' -Force"
            for /d %%D in (".gradle_tools\jdk8_temp\*") do move "%%D" ".gradle_tools\jdk8" >nul
            rmdir /s /q ".gradle_tools\jdk8_temp" 2>nul
            del /q ".gradle_tools\jdk8.zip" 2>nul
        )
    )
    if exist ".gradle_tools\jdk8\bin\javac.exe" (
        set "JAVA_HOME=%~dp0.gradle_tools\jdk8"
        set "PATH=%~dp0.gradle_tools\jdk8\bin;%PATH%"
        echo [OK] Портативна JDK 8 успішно підключена!
    ) else (
        echo [ПОМИЛКА] Не вдалося завантажити JDK 8. Перевірте підключення до інтернету.
        pause
        exit /b 1
    )
)

REM 2. Перевірка або автоматичне завантаження Gradle 4.10.3
echo.
echo [2/3] Підготовка системи збірки Gradle 4.10.3...
if not exist ".gradle_tools\gradle-4.10.3\bin\gradle.bat" (
    mkdir ".gradle_tools" 2>nul
    echo Завантаження Gradle 4.10.3...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-4.10.3-bin.zip' -OutFile '.gradle_tools\gradle.zip'"
    if exist ".gradle_tools\gradle.zip" (
        echo Розпакування Gradle 4.10.3...
        powershell -Command "Expand-Archive -Path '.gradle_tools\gradle.zip' -DestinationPath '.gradle_tools' -Force"
        del /q ".gradle_tools\gradle.zip" 2>nul
    )
)

if not exist ".gradle_tools\gradle-4.10.3\bin\gradle.bat" (
    echo [ПОМИЛКА] Не вдалося завантажити Gradle 4.10.3. Перевірте інтернет.
    pause
    exit /b 1
)

REM 3. Компіляція мода
echo.
echo [3/3] Запуск компіляції мода (перший запуск може тривати 3-7 хвилин)...
echo =====================================================================
call ".gradle_tools\gradle-4.10.3\bin\gradle.bat" setupDecompWorkspace build --no-daemon

echo.
if exist "build\libs\hbmcompass-1.12.2-1.0.jar" (
    echo =====================================================================
    echo [УСПІХ!] Мод успішно скомпільовано!
    echo Готовий файл знаходиться тут:
    echo %~dp0build\libs\hbmcompass-1.12.2-1.0.jar
    echo.
    echo Просто скопіюйте його у папку .minecraft\mods разом із вашим HBM Extended!
    echo =====================================================================
    explorer "%~dp0build\libs"
) else (
    echo =====================================================================
    echo [ПОМИЛКА] Щось пішло не так під час компіляції. Перегляньте лог вище.
    echo =====================================================================
)
pause
