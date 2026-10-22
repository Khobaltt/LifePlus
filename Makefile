# tools
JAVAC := javac
JAVA := java

# source files
SOURCES := src/main/java/Main.java \
           $(wildcard src/main/java/core/rect/*.java) \
           $(wildcard src/main/java/ui/rect/*.java) \
		   $(wildcard src/main/java/ui/window/*.java) \
		   $(wildcard src/test/ui/*.java)

.PHONY: all compile run clean

all: compile

# compile the project into out
compile:
	mkdir -p out
	$(JAVAC) -d out $(SOURCES)

# compile and run the project
run: compile
	$(JAVA) -cp out main.java.Main

# remove compiled files
clean:
	rm -rf out