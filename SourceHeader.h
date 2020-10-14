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
#define JNIEXPORT
#else
#define JNIEXPORT\
    extern "C"
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
    JNIEXPORT ProcessCPUTime(ULARGE_INTEGER procKernelTime, ULARGE_INTEGER procUserTime, ULARGE_INTEGER sysIdleTime, ULARGE_INTEGER sysKernelTime, ULARGE_INTEGER sysUserTime) : procKernelTime(procKernelTime), procUserTime(procUserTime), sysIdleTime(sysIdleTime), sysKernelTime(sysKernelTime), sysUserTime(sysUserTime) {};

#ifdef  DEBUG
    void print()
    {
        std::cout << "ProcKernelTime: " << procKernelTime.QuadPart << "\t\n";// << procKernelTime.HighPart << "\t" << procKernelTime.LowPart << "\n";
        std::cout << "ProcUserTime: " << procUserTime.QuadPart << "\t\n";// << procUserTime.HighPart << "\t" << procUserTime.LowPart << "\n";
        std::cout << "SysIdleTime: " << sysIdleTime.QuadPart << "\t\n";// << sysIdleTime.HighPart << "\t" << sysIdleTime.LowPart << "\n";
        std::cout << "SysKernelTime: " << sysKernelTime.QuadPart << "\t\n";// << sysKernelTime.HighPart << "\t" << sysKernelTime.LowPart << "\n";
        std::cout << "SysUserTime: " << sysUserTime.QuadPart << "\t\n";// << sysUserTime.HighPart << "\t" << sysUserTime.LowPart << "\n";
    };
#endif //DEBUG
};

class JNIWindowsSnapAdapter
{
private:
    std::unordered_map<int, ProcessCPUTime> prevProcessesCPUTime;
    HANDLE hProcessesSnap;
    HANDLE hProcess;
    PROCESSENTRY32 pe32;
public:
    JNIEXPORT JNIWindowsSnapAdapter() { hProcessesSnap = NULL; hProcess = NULL;  pe32 = {}; };
    //Throws SnapError on failure
    JNIEXPORT ~JNIWindowsSnapAdapter();
    JNIEXPORT unsigned long getCurProcID() { return pe32.th32ProcessID; };
    //Length of TCHAR is 260, TCHAR = wchar_t
    JNIEXPORT TCHAR* getCurProcName() { return pe32.szExeFile; };
    JNIEXPORT int getCurProcThreadCnt() { return pe32.cntThreads; };
    //Function can't get name of a priveleged (run as administrator) process. It opens process inside to get it's name
    //Returns "" on failure
    JNIEXPORT char* getProgrammNameByActiveWindow();
    //Function does not presents result on a first call. But on each call it changes prevProcessesCPUTime (if windows processes exist)
    //When prevProcessCPUTime contains non-null data about process it counts arithmetical mean of CPU load and returns
    //Bug may occur if process had an id, then it was closed and other process with the same id was opened
    //Returns -1 on error
    JNIEXPORT double getCpuLoadByProcess();
    //Returns working set of a process (if simplify, return value is a number that represents volume.
    //Actually it's less but approx equals to actual listed in RAM). More here: https://stackoverflow.com/a/5406063/12287688
    //Returns SIZE_MAX on failure
    JNIEXPORT size_t getRAMLoadByProcess();
    //Decide if we need all processes or parent only
    //Gets next process entry, opens next proess. Ignores system idle process
    //Returns false if no next processes available
    JNIEXPORT bool toNextProcess();
    //Snap is an immediate image of system that includes processes
    //Function updates snap and calls toNextProcess so hProcess is valid
    //Returns false on failure
    JNIEXPORT bool updateSnap();
};