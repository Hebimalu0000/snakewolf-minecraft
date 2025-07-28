@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

:: --- 設定項目 ---
set "APP_NAME=Offline Developer Edition Minecraft"
set "INSTALL_DIR=%LOCALAPPDATA%\ODE_Minecraft"
set "UNINSTALLER_PATH=%~f0"
set "TEMP_DESTROYER=%TEMP%\ODE_Minecraft_Installer_destroy.bat"

:: --- プロンプト ---
echo.
echo %APP_NAME% をアンインストールします。
echo.
echo 以下のディレクトリとショートカットを削除します。
echo   - インストールディレクトリ: %INSTALL_DIR%
echo   - デスクトップのショートカット
echo   - スタートメニューのショートカット
echo.

set /p UNINSTALL_CONFIRM="本当にアンインストールしますか？ (y/n) "
if /i not "!UNINSTALL_CONFIRM!"=="y" (
    echo アンインストールを中止しました。
    goto :eof
)

echo アンインストールを開始します...

:: --- インストールディレクトリの削除 ---
if exist "%INSTALL_DIR%" (
    echo インストールディレクトリを削除中: "%INSTALL_DIR%"
    rmdir /s /q "%INSTALL_DIR%"
    if errorlevel 1 (
        echo エラー: ディレクトリの削除に失敗しました。管理者権限が必要かもしれません。
    ) else (
        echo ディレクトリの削除が完了しました。
    )
) else (
    echo インストールディレクトリは存在しません: "%INSTALL_DIR%"
)

:: --- ショートカットの削除 ---
echo ショートカットを削除中...
set "LNK_NAME=%APP_NAME%.lnk"
set "UNINSTALL_LNK_NAME=アンインストール.lnk"
set "START_MENU_FOLDER=%APPDATA%\Microsoft\Windows\Start Menu\Programs\%APP_NAME%"

if exist "%USERPROFILE%\Desktop\%LNK_NAME%" (
    del "%USERPROFILE%\Desktop\%LNK_NAME%"
    echo デスクトップのショートカットを削除しました。
) else (
    echo デスクトップのショートカットは存在しません: "%USERPROFILE%\Desktop\%LNK_NAME%"
)

if exist "%START_MENU_FOLDER%" (
    del "%START_MENU_FOLDER%\%LNK_NAME%" > nul 2>&1
    del "%START_MENU_FOLDER%\%UNINSTALL_LNK_NAME%" > nul 2>&1
    rmdir "%START_MENU_FOLDER%" > nul 2>&1
    echo スタートメニューのショートカットとフォルダを削除しました。
) else (
    echo スタートメニューのフォルダは存在しません: "%START_MENU_FOLDER%"
)

echo.
echo %APP_NAME% のアンインストールが完了しました。

echo.
pause
endlocal
goto :eof
