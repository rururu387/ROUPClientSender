packageName=com/company/
jdkPath=C:/Program Files/Java/jdk-15/
outPath=./out/MakeOutput/
sourcePath=./src/CPPSource/

all : build

build : ClientMainClass.dll
	
com_company_JNIAdapter.o :
	clang++ $(sourcePath)com_company_JNIAdapter.cpp -c -std=c++17 -lUser32 -I"$(jdkPath)include" -I"$(jdkPath)include/win32" -I"$(sourcePath)" -o $(outPath)com_company_JNIAdapter.o -std=c++17

ClientMainClass.dll : com_company_JNIAdapter.o
	clang++ $(outPath)com_company_JNIAdapter.o -shared -lUser32 -o $(outPath)ClientMainClass.dll -std=c++17
	
cleanAll :
	rm $(outPath)*.o $(outPath)*.dll $(sourcePath)com_company_JNIAdapter.h $(outPath)*.exp $(outPath)*.lib

clean :
	rm ./*.o

#file *.dll to get to know which platform dll for