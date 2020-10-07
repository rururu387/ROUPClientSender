#pragma once
#include <windows.h>
#include <pdh.h>
#include <tlhelp32.h>
#include <string>

#define DEBUG
#ifdef  DEBUG
#include <iostream>
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

class JNIWindowsSnapAdapter
{
private:
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
    //{ return Process32Next(hProcessesSnap, &pe32); OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pe32.th32ProcessID); };
    char* getProgrammNameByActiveWindow();
    int getCpuLoadByProcess();
    int64_t getRAMLoadByProcess();
    bool updateSnap();
};

/*class JNIWindowsQueryAdapter
{
private:
    DWORD_PTR queryNumPtr;
    PDH_HQUERY queryPtr;

public:
    JNIWindowsQueryAdapter() { queryNumPtr = NULL; queryPtr = {}; };
    ~JNIWindowsQueryAdapter();
    bool openRAMCPUQuery();
};*/