packageName=com/company/
java15Path=C:/Program Files/Java/jdk-15/bin/java.exe
libericaJava15Path=C:/Program Files/BellSoft/LibericaJRE-15-Full/bin/java.exe
javaFXPath=C:/Program Files/Java/javafx-sdk-15/
jdkPath=C:/Program Files/Java/jdk-15/
outPath=./out/MakeOutput/
sourcePath=./src/CPPSource/
executablePath=C:/Users/Lavrentiy_Gusev/Desktop/executables/

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
	
buildJar :
	cp ./src/libs/* "$(executablePath)src/libs/"
	cp ./out/artifacts/ROUP/ROUP.jar "$(executablePath)"
	
runJar : buildJar
	"$(java15Path)" --module-path "$(javaFXPath)lib" --add-modules=javafx.controls,javafx.fxml -jar "$(executablePath)ROUP.jar"

cleanExecutables :
	find $(executablePath) -type f -not -name '*.xml' -not -name '*.ico' -delete

#file *.dll to get to know which platform dll for