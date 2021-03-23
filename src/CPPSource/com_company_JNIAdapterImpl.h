#pragma once
#include <unordered_map>
#define UNICODE
#define WINVER 0x0601
#define _WIN32_WINNT 0x0601
#include <windows.h>
#include <pdh.h>
#include <psapi.h>
#include <tlhelp32.h>
#include <string>

//#define DEBUG
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
    JNIWindowsSnapAdapter() { hProcessesSnap = NULL; hProcess = NULL;  pe32 = {}; };
    //Throws SnapError on failure
    ~JNIWindowsSnapAdapter();

    unsigned long getCurProcID() { return pe32.th32ProcessID; };
    //Length of TCHAR is 260, TCHAR = wchar_t
    wchar_t* getCurProcName() { return pe32.szExeFile; };
    int getCurProcThreadCnt() { return pe32.cntThreads; };
    //Function can't get name of a priveleged (run as administrator) process. It opens process inside to get it's name
    //Returns "" on failure
    wchar_t* getProgramNameByActiveWindow();
    //Function does not presents result on a first call. But on each call it changes prevProcessesCPUTime (if windows processes exist)
    //When prevProcessCPUTime contains non-null data about process it counts arithmetical mean of CPU load and returns
    //Bug may occur if process had an id, then it was closed and other process with the same id was opened
    //Returns -1 on error
    double getCpuLoadByProcess();
    //Returns working set of a process (if simplify, return value is a number that represents volume.
    //Actually it's less but approx equals to actual listed in RAM). More here: https://stackoverflow.com/a/5406063/12287688
    //Returns SIZE_MAX on failure
    size_t getRAMLoadByProcess();
    //Decide if we need all processes or parent only
    //Gets next process entry, opens next proess. Ignores system idle process
    //Returns false if no next processes available
    bool toNextProcess();
    //Snap is an immediate image of system that includes processes
    //Function updates snap and calls toNextProcess so hProcess is valid
    //Returns false on failure
    bool updateSnap();
};

JNIWindowsSnapAdapter::~JNIWindowsSnapAdapter()
{
	bool errorFlag = 0;
    SnapError error = SnapError(L"");
	if (hProcess != NULL)
    {
		if (!CloseHandle(hProcess))
        {
			error = SnapError(L"Closing process handle in destructor");
            errorFlag = 1;
        }
    }
	if (hProcessesSnap != 0 && !CloseHandle(hProcessesSnap) && errorFlag == 0)
    {
		error = SnapError(L"Closing processes snap in destructor");
        errorFlag = 1;
    }
	if (errorFlag != 0)
    {
        MessageBox(NULL, error.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
    }
};

bool JNIWindowsSnapAdapter::updateSnap()
{
	if (hProcessesSnap != NULL)
		CloseHandle(hProcessesSnap);
	// Take a snapshot of all processes in the system.
	hProcessesSnap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
	if (hProcessesSnap == INVALID_HANDLE_VALUE)
	{
		throw SnapError(L"CreateToolhelp32Snapshot (of processes)");
		return(FALSE);
	}

	pe32.dwSize = sizeof(PROCESSENTRY32);

	// Retrieve information about the first process
	if (!Process32First(hProcessesSnap, &pe32))
	{
		throw SnapError(L"Getting Process32First");
		return(FALSE);
	}
	this->toNextProcess();
	return(TRUE);
}

bool JNIWindowsSnapAdapter::toNextProcess()
{
	try
	{
		if (hProcess != NULL)
			if (!CloseHandle(hProcess))
				throw SnapError(L"Closing handle to a process");
		hProcess = NULL;
		if (Process32Next(hProcessesSnap, &pe32))
		{
			if (&(pe32.th32ProcessID) == 0)
				toNextProcess();
			this->hProcess = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pe32.th32ProcessID);
			if (hProcess == NULL)
			{
				//std::wcerr << "Opening process " << pe32.th32ProcessID << " failed with error " << GetLastError() << " and it's ok in most cases =)\n";
			}
			return true;
		}
		else
		{
			return false;
		}
	}
	catch (SnapError e)
	{
		MessageBox(NULL, e.what().c_str(), L"Warning", MB_ICONEXCLAMATION | MB_OK);
		return true;
		//if a variable is set to scilent
		//std::wcerr << e.what();
	}
}

wchar_t* JNIWindowsSnapAdapter::getProgramNameByActiveWindow()
{
	DWORD procId = 0;
	GetWindowThreadProcessId(GetForegroundWindow(), &procId);
	LPSTR procActiveWindow[PROGRAMMNAMEMAXLEN];
	HANDLE hActiveProcess = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, procId);
	try
	{
		DWORD bufLen = PROGRAMMNAMEMAXLEN;
		WCHAR buffer[PROGRAMMNAMEMAXLEN];
		if (QueryFullProcessImageName(hActiveProcess, 0, buffer, &bufLen))
		{
			wchar_t* name = new wchar_t[PROGRAMMNAMEMAXLEN];
			int p = 0, lastSeparator = 0;
			while (p != PROGRAMMNAMEMAXLEN && buffer[p] != L'\0')
			{
				if (buffer[p] == L'\\' || buffer[p] == L'/')
					lastSeparator = p;
				p++;
			}
			int i = 0;
			p = lastSeparator + 1;
			for (i = 0; p < PROGRAMMNAMEMAXLEN && buffer[p] != L'\0'; i++, p++)
			{
				name[i] = buffer[p];
			}
			name[i] = L'\0';
			return name;
		}
		else
		{
			wchar_t* retVal = new wchar_t[31];
			retVal = std::wcscpy(retVal, L"Foreground process query error");
			return retVal;
			//throw SnapError(L"Getting process name by active window error GetModuleBaseNameA -");
			//if a variable is set to scilent
			//std::wcerr << "Getting process name by active window error GetModuleBaseNameA faiked in process " << procId;
		}
	}
	catch (SnapError e)
	{
		wchar_t* retVal = new wchar_t[16];
		retVal = std::wcscpy(retVal, L"Unknown program");
		return retVal;
		//MessageBox(NULL, e.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
		//if a variable is set to scilent
		//std::wcerr << e.what();
		//return const_cast<wchar_t*>(L"");
	}
	wchar_t* retVal = new wchar_t[14];
	retVal = std::wcscpy(retVal, L"Unkown error!");
	return retVal;
}

double JNIWindowsSnapAdapter::getCpuLoadByProcess()
{
	double retVal = -1;
	try
	{
		_FILETIME procCreateTime = {};
		_FILETIME procExitTime = {};
		_FILETIME procKernelTime = {};
		_FILETIME procUserTime = {};

		if (hProcess == NULL)
		{
			//std::cerr << "And so could not get cpu usage of process " << pe32.th32ProcessID << " named " << pe32.szExeFile << "\n";
			return -1;
		}
		if (!GetProcessTimes(hProcess, &procCreateTime, &procExitTime, &procKernelTime, &procUserTime))
		{
			throw SnapError(L"Getting time of process being busy");
		}
		_FILETIME sysIdleTime = {};
		_FILETIME sysKernelTime = {};
		_FILETIME sysUserTime = {};

		ULARGE_INTEGER procKernelInteger = {};
		ULARGE_INTEGER procUserInteger = {};
		ULARGE_INTEGER sysIdleInteger = {};
		ULARGE_INTEGER sysKernelInteger = {};
		ULARGE_INTEGER sysUserInteger = {};

		if (!GetSystemTimes(&sysIdleTime, &sysKernelTime, &sysUserTime))
		{
			throw SnapError(L"Getting time of system");
		}

		procKernelInteger.HighPart = procKernelTime.dwHighDateTime;
		procKernelInteger.LowPart = procKernelTime.dwLowDateTime;

		procUserInteger.HighPart = procUserTime.dwHighDateTime;
		procUserInteger.LowPart = procUserTime.dwLowDateTime;

		sysIdleInteger.HighPart = sysIdleTime.dwHighDateTime;
		sysIdleInteger.LowPart = sysIdleTime.dwLowDateTime;

		sysKernelInteger.HighPart = sysIdleTime.dwHighDateTime;
		sysKernelInteger.LowPart = sysIdleTime.dwLowDateTime;

		sysUserInteger.HighPart = sysUserTime.dwHighDateTime;
		sysUserInteger.LowPart = sysUserTime.dwLowDateTime;

		//Could use GetProcessId(hProcess) instead, but chose speed
		auto processIt = prevProcessesCPUTime.find(pe32.th32ProcessID);
		if (processIt != prevProcessesCPUTime.end())
		{
			//std::cout << "\n\nBefore:\n";
			//processIt->second.print();

			auto procKernelInterval = procKernelInteger.QuadPart - processIt->second.procKernelTime.QuadPart;
			auto procUserInterval = procUserInteger.QuadPart - processIt->second.procUserTime.QuadPart;
			auto sysIdleInterval = sysIdleInteger.QuadPart - processIt->second.sysIdleTime.QuadPart;
			auto sysKernelInterval = sysKernelInteger.QuadPart - processIt->second.sysKernelTime.QuadPart;
			auto sysUserIntercals = sysUserInteger.QuadPart - processIt->second.sysUserTime.QuadPart;
			retVal = static_cast<double>(procKernelInterval + procUserInterval) / static_cast<double>(sysIdleInterval + sysKernelInterval + sysUserIntercals) * 100;

			processIt->second = ProcessCPUTime(procKernelInteger, procUserInteger, sysIdleInteger, sysKernelInteger, sysUserInteger);
			//std::cout << "After:\n";
			//processIt->second.print();
		}
		else
		{
			prevProcessesCPUTime.emplace(pe32.th32ProcessID, ProcessCPUTime(procKernelInteger, procUserInteger, sysIdleInteger, sysKernelInteger, sysUserInteger));
			retVal = -1;
		}
		return retVal;
	}
	catch (SnapError e)
	{
		//MessageBox(NULL, e.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
		return -1;
	}
	return -1;
}

size_t JNIWindowsSnapAdapter::getRAMLoadByProcess()
{
	try
	{
		PROCESS_MEMORY_COUNTERS_EX pmc;
		if (!GetProcessMemoryInfo(hProcess, (PROCESS_MEMORY_COUNTERS*)&pmc, sizeof(pmc)))
		{
			throw SnapError(L"Getting ram usage of process");
		}
		//SIZE_T virtualMemProcessUsed = pmc.PrivateUsage / 1048576;
		SIZE_T physMemProcessUsed = pmc.WorkingSetSize / 1048576;
		return physMemProcessUsed;
	}
	catch (SnapError e)
	{
		//std::cerr << "And so " << e.what() << pe32.th32ProcessID << " named " << pe32.szExeFile << "\n";
		return SIZE_MAX;
	}
	return SIZE_MAX;
}

SnapError::SnapError(const wchar_t* what)
{
	eNum = GetLastError();
	FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL, eNum,
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), // Default language
		sysMsg, 256, NULL);

	TCHAR* p = sysMsg;

	while ((*p > 31) || (*p == 9))
		++p;
	do { *p-- = 0; } while ((p >= sysMsg) &&
		((*p == '.') || (*p < 33)));

	this->whatStr = what;
	this->whatStr += L" failed with error " + std::to_wstring(eNum) + L"\n" + sysMsg;
}