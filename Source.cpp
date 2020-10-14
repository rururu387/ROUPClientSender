#include "SourceHeader.h"

JNIWindowsSnapAdapter::~JNIWindowsSnapAdapter()
{
	SnapError* errorPtr = NULL;
	if (hProcess != NULL)
		if (!CloseHandle(hProcess))
			errorPtr = &SnapError(L"Closing process handle in destructor");
	if (!CloseHandle(hProcessesSnap) && errorPtr == NULL)
		errorPtr = &SnapError(L"Closing processes snap in destructor");
	if (errorPtr != NULL)
		throw* errorPtr;
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
			if (&pe32.th32ProcessID == 0)
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

char* JNIWindowsSnapAdapter::getProgrammNameByActiveWindow()
{
	DWORD procId = 0;
	GetWindowThreadProcessId(GetForegroundWindow(), &procId);
	LPSTR procActiveWindow[PROGRAMMNAMEMAXLEN];
	HANDLE hActiveProcess = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, procId);
	try
	{
		DWORD bufLen = PROGRAMMNAMEMAXLEN;
		CHAR buffer[PROGRAMMNAMEMAXLEN];
		if (QueryFullProcessImageNameA(hActiveProcess, 0, buffer, &bufLen))
		{
			char name[PROGRAMMNAMEMAXLEN];
			int p = 0, lastSeparator = 0;
			while (p != PROGRAMMNAMEMAXLEN && buffer[p] != '\0')
			{
				if (buffer[p] == '\\' || buffer[p] == '\/')
					lastSeparator = p;
				p++;
			}
			int i = 0;
			p = lastSeparator + 1;
			for (i = 0; p < PROGRAMMNAMEMAXLEN && buffer[p] != '\0'; i++, p++)
			{
				name[i] = buffer[p];
			}
			name[i] = '\0';
			return name;
		}
		else
		{
			throw SnapError(L"Getting process name by active window error GetModuleBaseNameA -");
			//if a variable is set to scilent
			//std::wcerr << "Getting process name by active window error GetModuleBaseNameA faiked in process " << procId;
		}
	}
	catch (SnapError e)
	{
		MessageBox(NULL, e.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
		//if a variable is set to scilent
		//std::wcerr << e.what();
		return const_cast<char*>("");
	}
	char* str = const_cast<char*>("");
	return str;
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
		MessageBox(NULL, e.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
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

int main()
{
	setlocale(LC_ALL, "RUS");

	JNIWindowsSnapAdapter adapter = JNIWindowsSnapAdapter();
	try
	{
		adapter.updateSnap();
	}
	catch (SnapError e)
	{
		MessageBox(NULL, e.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
		//std::wcerr << e.what();
	}
	do
	{
		//if (wcscmp(adapter.getCurProcName(), L"chrome.exe") == 0)
		{
			std::wcout << "ID: " << adapter.getCurProcID() << "\nName: " << adapter.getCurProcName() << "\nThreads: " << adapter.getCurProcThreadCnt() << "\nRAM usage: " << adapter.getRAMLoadByProcess() << "MB\n";
			adapter.getCpuLoadByProcess();
			Sleep(100);
			double d = adapter.getCpuLoadByProcess();
			std::wcout << "Interval CPU usage: " << d << "%\n\n-------------\n";
		}
	} while (adapter.toNextProcess() != 0);
	char procName[PROGRAMMNAMEMAXLEN];
	std::cout << "Active window process name: " << adapter.getProgrammNameByActiveWindow() << "\n";
	return 0;
}