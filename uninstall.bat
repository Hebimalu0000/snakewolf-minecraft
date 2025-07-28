@echo off
chcp 932 > nul
setlocal enabledelayedexpansion

:: --- 設定項目 ---
set "APP_NAME=Offline Developer Edition Minecraft"
set "INSTALL_DIR=%LOCALAPPDATA%\ODE_Minecraft"
set "UNINSTALLER_TEMP_DIR=%TEMP%\%APP_NAME%_Uninstaller"
set "UNINSTALLER_NAME=temp_uninstall.bat"

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

:: --- 自分自身を一時フォルダにコピーして再起動 ---
if not exist "%UNINSTALLER_TEMP_DIR%" mkdir "%UNINSTALLER_TEMP_DIR%"
copy "%~dpnx0" "%UNINSTALLER_TEMP_DIR%\%UNINSTALLER_NAME%" > nul

echo アンインストーラーを一時フォルダで再起動します...
start "" "%UNINSTALLER_TEMP_DIR%\%UNINSTALLER_NAME%" "%INSTALL_DIR%" "%APP_NAME%"
goto :eof

:main_procedure
echo.
echo メインのアンインストール処理を実行中...

:: --- インストールディレクトリの削除 ---
if exist "%~1" (
    echo インストールディレクトリを削除中: %~1
    rmdir /s /q "%~1"
    if errorlevel 1 (
        echo エラー: ディレクトリの削除に失敗しました。管理者権限が必要かもしれません。
    ) else (
        echo ディレクトリの削除が完了しました。
    )
) else (
    echo インストールディレクトリは既に存在しません。
)

:: --- ショートカットの削除 ---
echo ショートカットを削除中...
set "LNK_NAME=%~2.lnk"
set "UNINSTALL_LNK_NAME=アンインストール.lnk"
set "START_MENU_FOLDER=%APPDATA%\Microsoft\Windows\Start Menu\Programs\%~2"

:: デスクトップのショートカットを削除
if exist "%USERPROFILE%\Desktop\%LNK_NAME%" (
    del "%USERPROFILE%\Desktop\%LNK_NAME%"
    echo デスクトップのショートカットを削除しました。
)

:: スタートメニューのショートカットとフォルダを削除
if exist "%START_MENU_FOLDER%" (
    del "%START_MENU_FOLDER%\%LNK_NAME%" > nul 2>&1
    del "%START_MENU_FOLDER%\%UNINSTALL_LNK_NAME%" > nul 2>&1
    rmdir "%START_MENU_FOLDER%"
    echo スタートメニューのショートカットとフォルダを削除しました。
)

echo.
echo %~2 のアンインストールが完了しました。

:: --- 自己削除 ---
echo アンインストーラーを自己削除します...
(
    echo @ping 127.0.0.1 -n 2 > nul
    echo @del "%~dpnx0"
    echo @rmdir /s /q "%UNINSTALLER_TEMP_DIR%"
) > "%TEMP%\self_destruct.bat"
start "" /b "%TEMP%\self_destruct.bat"

echo.
pause
endlocal
goto :eof