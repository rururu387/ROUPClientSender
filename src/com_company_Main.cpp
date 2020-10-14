#include "com_company_Main.h"
#include <iostream>

JNIEXPORT void JNICALL Java_com_company_Main_showString(jstring myString)
{
    std::cout << myString;
}