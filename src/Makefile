javaFilePath=./com/company/

all: build

build: Main.dll classes

classes: $(javaFilePath)ClientMainClass.java
	javac -h . $(javaFilePath)ClientMainClass.java
	
com_company_Main.o : com_company_Main.cpp com_company_Main.h
	g++ com_company_Main.cpp -c -std=c++17 -I"/cygdrive/c/Program Files/Java/jdk-15/include" -I"/cygdrive/c/Program Files/Java/jdk-15/include/win32"

Main.dll: com_company_Main.o
	g++ ./com_company_Main.o -shared -o Main.dll -Wl,--add-stdcall-alias
	
cleanAll:
	rm ./*.o ./*.dll ./*.class $(javaFilePath)*.o $(javaFilePath)*.dll $(javaFilePath)*.class

clean:
	rm ./*.o
	
run :
	java -cp . -Djava.library.path="C:/Users/Lavrentiy_Gusev/IdeaProjects/clientSender/src/" com.company.ClientMainClass
#file *.dll to get to know which platform dll for