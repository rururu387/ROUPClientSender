packageName=com/company/
javaFilePath=./src/$(packageName)
jdkPath=C:/Program Files/Java/jdk-15/
outPath=./out/MakeOutput/

java : classes run

cpp : ClientMainClass.dll run

all : build

build : classes ClientMainClass.dll

classes: $(javaFilePath)ClientMainClass.java
	javac -h ./src $(javaFilePath)ClientMainClass.java $(javaFilePath)JNIAdapter.java -d $(outPath)
	
com_company_JNIAdapter.o :
	clang++ ./src/com_company_JNIAdapter.cpp -c -std=c++17 -lUser32 -I"$(jdkPath)include" -I"$(jdkPath)include/win32" -I"./src/" -o $(outPath)com_company_JNIAdapter.o -std=c++17

ClientMainClass.dll : com_company_JNIAdapter.o
	clang++ $(outPath)com_company_JNIAdapter.o -shared -lUser32 -o $(outPath)ClientMainClass.dll -std=c++17
	
cleanAll :
	rm $(outPath)*.o $(outPath)*.dll $(outPath)$(packageName)*.class ./src/com_company_ClientMainClass.h $(outPath)*.exp $(outPath)*.lib

clean :
	rm ./*.o
	
run :
	java -cp $(outPath) -Djava.library.path="$(outPath)" com.company.ClientMainClass

#file *.dll to get to know which platform dll for