import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

public class SystemEnvChecker {

    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * 1024L * 1024L;

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "GBK"));
            System.setErr(new java.io.PrintStream(System.err, true, "GBK"));
        } catch (Exception ignored) {}

        printSection("=== SYSTEM ENVIRONMENT ===");
        System.out.println("OS Name       : " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("OS Arch       : " + System.getProperty("os.arch"));
        System.out.println("CPU Cores     : " + Runtime.getRuntime().availableProcessors());
        System.out.println("User Name     : " + System.getProperty("user.name"));
        System.out.println("Working Dir   : " + System.getProperty("user.dir"));
        System.out.println("Temp Dir      : " + System.getProperty("java.io.tmpdir"));

        printSection("=== JAVA RUNTIME ===");
        System.out.println("Java Version  : " + System.getProperty("java.version"));
        System.out.println("Java Vendor   : " + System.getProperty("java.vendor"));
        System.out.println("JVM Name      : " + System.getProperty("java.vm.name"));
        System.out.println("JVM Version   : " + System.getProperty("java.vm.version"));
        System.out.println("File Encoding : " + System.getProperty("file.encoding"));
        System.out.println("Default Charset: " + Charset.defaultCharset().name());
        System.out.println("Locale        : " + Locale.getDefault().toString());

        printSection("=== JVM INPUT ARGUMENTS ===");
        RuntimeMXBean runtimeMx = ManagementFactory.getRuntimeMXBean();
        runtimeMx.getInputArguments().forEach(arg -> System.out.println("  " + arg));

        printSection("=== MEMORY USAGE ===");
        MemoryMXBean memMx = ManagementFactory.getMemoryMXBean();

        MemoryUsage heap = memMx.getHeapMemoryUsage();
        System.out.println("[Heap Memory]");
        System.out.printf("  Used / Max  : %d MB / %d MB (%.1f%%)%n",
                heap.getUsed() / MB, heap.getMax() / MB,
                heap.getMax() > 0 ? (heap.getUsed() * 100.0 / heap.getMax()) : 0);
        System.out.printf("  Committed   : %d MB%n", heap.getCommitted() / MB);

        MemoryUsage nonHeap = memMx.getNonHeapMemoryUsage();
        System.out.println("[Non-Heap Memory]");
        System.out.printf("  Used / Committed : %d MB / %d MB%n",
                nonHeap.getUsed() / MB, nonHeap.getCommitted() / MB);

        Runtime rt = Runtime.getRuntime();
        printSection("=== RUNTIME MEMORY OVERVIEW ===");
        System.out.printf("Total Allocated : %d MB%n", rt.totalMemory() / MB);
        System.out.printf("Free Memory     : %d MB%n", rt.freeMemory() / MB);
        System.out.printf("Max Available   : %d MB%n", rt.maxMemory() / MB);
        System.out.printf("Actual Consumed : %d MB%n", (rt.totalMemory() - rt.freeMemory()) / MB);

        printSection("=== KEY ENVIRONMENT VARIABLES ===");
        var envKeys = new String[]{"JAVA_HOME", "PATH", "FABRIC_LOADER_VERSION", "MOD_DEV_DIR"};
        Map<String, String> env = System.getenv();
        for (var key : envKeys) {
            var val = env.get(key);
            if (val != null) {
                var display = key.equals("PATH") && val.length() > 80
                        ? val.substring(0, 80) + "..." : val;
                System.out.println(key + " = " + display);
            } else {
                System.out.println(key + " = [NOT SET]");
            }
        }

        printSection("=== DISK SPACE ===");
        for (File root : File.listRoots()) {
            long total = root.getTotalSpace();
            long usable = root.getUsableSpace();
            if (total > 0) {
                System.out.printf("%s  Total: %d GB | Usable: %d GB (%.1f%%)%n",
                        root.getAbsolutePath(), total / GB, usable / GB, usable * 100.0 / total);
            }
        }

        System.out.println("\n[OK] Diagnostics complete. To export report:");
        System.out.println("     java -Dfile.encoding=GBK SystemEnvChecker > env_report.txt");
    }

    private static void printSection(String title) {
        System.out.println("\n" + title);
        System.out.println("-".repeat(title.length()));
    }
}