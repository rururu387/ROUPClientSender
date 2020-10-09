#pragma once
#include <unordered_map>
#include <windows.h>
#include <pdh.h>
#include <psapi.h>
#include <tlhelp32.h>
#include <string>

#define DEBUG
#ifdef  DEBUG
#include <iostream>
#include <chrono>
template<typename T>
void tprint(char spacer, T arg)
{
    std::cout << arg;
    std::cout << "\n";
}

template<typename T, typename... Args>
void tprint(char spacer, T firstArg, Args... args)
{
    std::cout << firstArg << spacer;
    tprint(spacer, args...);
}
#else
#endif // DEBUG
#define PROGRAMMNAMEMAXLEN 260

class SnapError : std::exception
{
    DWORD eNum;
    TCHAR sysMsg[256];
    std::wstring whatStr;
public:
    SnapError(const wchar_t* what);
    const std::wstring what() { return whatStr; };
};

class ProcessCPUTime
{
public:
    ULARGE_INTEGER procKernelTime;
    ULARGE_INTEGER procUserTime;
    ULARGE_INTEGER sysIdleTime;
    ULARGE_INTEGER sysKernelTime;
    ULARGE_INTEGER sysUserTime;
public:
    ProcessCPUTime(ULARGE_INTEGER procKernelTime, ULARGE_INTEGER procUserTime, ULARGE_INTEGER sysIdleTime, ULARGE_INTEGER sysKernelTime, ULARGE_INTEGER sysUserTime) : procKernelTime(procKernelTime), procUserTime(procUserTime), sysIdleTime(sysIdleTime), sysKernelTime(sysKernelTime), sysUserTime(sysUserTime) {};

    void print()
    {
        std::cout << "ProcKernelTime: " << procKernelTime.QuadPart << "\t\n";// << procKernelTime.HighPart << "\t" << procKernelTime.LowPart << "\n";
        std::cout << "ProcUserTime: " << procUserTime.QuadPart << "\t\n";// << procUserTime.HighPart << "\t" << procUserTime.LowPart << "\n";
        std::cout << "SysIdleTime: " << sysIdleTime.QuadPart << "\t\n";// << sysIdleTime.HighPart << "\t" << sysIdleTime.LowPart << "\n";
        std::cout << "SysKernelTime: " << sysKernelTime.QuadPart << "\t\n";// << sysKernelTime.HighPart << "\t" << sysKernelTime.LowPart << "\n";
        std::cout << "SysUserTime: " << sysUserTime.QuadPart << "\t\n";// << sysUserTime.HighPart << "\t" << sysUserTime.LowPart << "\n";
    };
};

class JNIWindowsSnapAdapter
{
private:
    std::unordered_map<int, ProcessCPUTime> prevProcessesCPUTime;
    HANDLE hProcessesSnap;
    HANDLE hProcess;
    PROCESSENTRY32 pe32;
public:
    JNIWindowsSnapAdapter() { hProcessesSnap = NULL; hProcess = NULL;  pe32 = {}; };
    ~JNIWindowsSnapAdapter();
    unsigned long getCurProcID() { return pe32.th32ProcessID; };
    //Length of TCHAR is 260, TCHAR = wchar_t
    TCHAR* getCurProcName() { return pe32.szExeFile; };
    int getCurProcThreadCnt() { return pe32.cntThreads; };
    //Decide if we need all processes or parent only
    int toNextProcessEntry();
    char* getProgrammNameByActiveWindow();
    double getCpuLoadByProcess();
    size_t getRAMLoadByProcess();
    bool updateSnap();
};