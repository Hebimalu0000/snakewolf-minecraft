@echo off
chcp 932 > nul
setlocal enabledelayedexpansion

:: --- �ݒ荀�� ---
set "APP_NAME=Offline Developer Edition Minecraft"
set "INSTALL_DIR=%LOCALAPPDATA%\ODE_Minecraft"
set "UNINSTALLER_TEMP_DIR=%TEMP%\%APP_NAME%_Uninstaller"
set "UNINSTALLER_NAME=temp_uninstall.bat"

:: --- �v�����v�g ---
echo.
echo %APP_NAME% ���A���C���X�g�[�����܂��B
echo.
echo �ȉ��̃f�B���N�g���ƃV���[�g�J�b�g���폜���܂��B
echo   - �C���X�g�[���f�B���N�g��: %INSTALL_DIR%
echo   - �f�X�N�g�b�v�̃V���[�g�J�b�g
echo   - �X�^�[�g���j���[�̃V���[�g�J�b�g
echo.

set /p UNINSTALL_CONFIRM="�{���ɃA���C���X�g�[�����܂����H (y/n) "
if /i not "!UNINSTALL_CONFIRM!"=="y" (
    echo �A���C���X�g�[���𒆎~���܂����B
    goto :eof
)

echo �A���C���X�g�[�����J�n���܂�...

:: --- �������g���ꎞ�t�H���_�ɃR�s�[���čċN�� ---
if not exist "%UNINSTALLER_TEMP_DIR%" mkdir "%UNINSTALLER_TEMP_DIR%"
copy "%~dpnx0" "%UNINSTALLER_TEMP_DIR%\%UNINSTALLER_NAME%" > nul

echo �A���C���X�g�[���[���ꎞ�t�H���_�ōċN�����܂�...
start "" "%UNINSTALLER_TEMP_DIR%\%UNINSTALLER_NAME%" "%INSTALL_DIR%" "%APP_NAME%"
goto :eof

:main_procedure
echo.
echo ���C���̃A���C���X�g�[�����������s��...

:: --- �C���X�g�[���f�B���N�g���̍폜 ---
if exist "%~1" (
    echo �C���X�g�[���f�B���N�g�����폜��: %~1
    rmdir /s /q "%~1"
    if errorlevel 1 (
        echo �G���[: �f�B���N�g���̍폜�Ɏ��s���܂����B�Ǘ��Ҍ������K�v��������܂���B
    ) else (
        echo �f�B���N�g���̍폜���������܂����B
    )
) else (
    echo �C���X�g�[���f�B���N�g���͊��ɑ��݂��܂���B
)

:: --- �V���[�g�J�b�g�̍폜 ---
echo �V���[�g�J�b�g���폜��...
set "LNK_NAME=%~2.lnk"
set "UNINSTALL_LNK_NAME=�A���C���X�g�[��.lnk"
set "START_MENU_FOLDER=%APPDATA%\Microsoft\Windows\Start Menu\Programs\%~2"

:: �f�X�N�g�b�v�̃V���[�g�J�b�g���폜
if exist "%USERPROFILE%\Desktop\%LNK_NAME%" (
    del "%USERPROFILE%\Desktop\%LNK_NAME%"
    echo �f�X�N�g�b�v�̃V���[�g�J�b�g���폜���܂����B
)

:: �X�^�[�g���j���[�̃V���[�g�J�b�g�ƃt�H���_���폜
if exist "%START_MENU_FOLDER%" (
    del "%START_MENU_FOLDER%\%LNK_NAME%" > nul 2>&1
    del "%START_MENU_FOLDER%\%UNINSTALL_LNK_NAME%" > nul 2>&1
    rmdir "%START_MENU_FOLDER%"
    echo �X�^�[�g���j���[�̃V���[�g�J�b�g�ƃt�H���_���폜���܂����B
)

echo.
echo %~2 �̃A���C���X�g�[�����������܂����B

:: --- ���ȍ폜 ---
echo �A���C���X�g�[���[�����ȍ폜���܂�...
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