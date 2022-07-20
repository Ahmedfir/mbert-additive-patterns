package additivepatterns.cli;

enum CliArgPrefix {

    FILE_INCLUDE_REQUEST("-in="),
    OUTPUT_DIR("-out=")    ;

    final String argPrefix;

    CliArgPrefix(String argPrefix) {
        this.argPrefix = argPrefix;
    }

    static CliArgPrefix startsWithPrefix(String arg) {
        for (CliArgPrefix cap : CliArgPrefix.values()) {
            if (arg.startsWith(cap.argPrefix)) {
                return cap;
            }
        }
        throw new IllegalArgumentException(arg);
    }

}
