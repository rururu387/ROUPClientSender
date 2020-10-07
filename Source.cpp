#include "SourceHeader.h"

JNIWindowsSnapAdapter::~JNIWindowsSnapAdapter()
{
    if (hProcess != NULL)
        if (!CloseHandle(hProcess))
            throw SnapError(L"Closing process handle in destructor");
    if (!CloseHandle(hProcessesSnap))
        throw SnapError(L"Closing processes snap in destructor");
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
    this->toNextProcessEntry();
    return(TRUE);
}


int JNIWindowsSnapAdapter::toNextProcessEntry()
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
                toNextProcessEntry();
            this->hProcess = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ, FALSE, pe32.th32ProcessID);
            if (hProcess == NULL)
            {
                //std::cerr << "Opening process " << pe32.th32ProcessID << " failed with error " << GetLastError() << " and it's ok in most cases =)\n";
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
        MessageBox(NULL, e.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
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
        }
    }
    catch (SnapError e)
    {
        MessageBox(NULL, e.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
        //if a variable is set to scilent
        //std::wcerr << e.what();
    }
    char* str = const_cast<char*>("");
    return str;
}

int JNIWindowsSnapAdapter::getCpuLoadByProcess()
{
    try
    {
        _FILETIME procCreateTime = {};
        _FILETIME procExitTime = {};
        _FILETIME procKernelTime = {};
        _FILETIME procUserTime = {};

        if (hProcess == NULL)
        {
            //std::cerr << "And so could not get cpu usage of process " << pe32.th32ProcessID << " named " << pe32.szExeFile;
            return -1;
        }
        if (!GetProcessTimes(hProcess, &procCreateTime, &procExitTime, &procKernelTime, &procUserTime))
        {
            throw SnapError(L"Getting time of process being busy");
        }
        _FILETIME sysIdleTime = {};
        _FILETIME sysKernelTime = {};
        _FILETIME sysUserTime = {};
        if (!GetSystemTimes(&sysIdleTime, &sysKernelTime, &sysUserTime))
        {
            throw SnapError(L"Getting time of system");
        }
        return (procKernelTime.dwLowDateTime + procKernelTime.dwHighDateTime + procUserTime.dwLowDateTime + procUserTime.dwHighDateTime) * 100 / (sysIdleTime.dwLowDateTime + sysIdleTime.dwHighDateTime + sysKernelTime.dwLowDateTime + sysKernelTime.dwHighDateTime + sysUserTime.dwLowDateTime + sysUserTime.dwHighDateTime);
    }
    catch (SnapError e)
    {
        MessageBox(NULL, e.what().c_str(), L"Warning!", MB_ICONEXCLAMATION | MB_OK);
    }
    return 0;
}

int64_t JNIWindowsSnapAdapter::getRAMLoadByProcess()
{

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

void EnableDebugPriv()
{
    HANDLE hToken;
    LUID luid;
    TOKEN_PRIVILEGES tkp;

    OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &hToken);

    LookupPrivilegeValue(NULL, SE_DEBUG_NAME, &luid);

    tkp.PrivilegeCount = 1;
    tkp.Privileges[0].Luid = luid;
    tkp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;

    AdjustTokenPrivileges(hToken, false, &tkp, sizeof(tkp), NULL, NULL);

    CloseHandle(hToken);
}

int main()
{
    setlocale(LC_ALL, "RUS");

    EnableDebugPriv();

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
        std::wcout << "ID: " << adapter.getCurProcID() << "\nName: " << adapter.getCurProcName() << "\nThreads: " << adapter.getCurProcThreadCnt() << "\nCPU usage: " << adapter.getCpuLoadByProcess() << "%" << "\n-------------\n";
        //Sleep(500);
    } while (adapter.toNextProcessEntry() != 0);
    char procName[PROGRAMMNAMEMAXLEN];
    std::cout << "Active window process name: " << adapter.getProgrammNameByActiveWindow() << "\n";
    return 0;
}