public class JavaEnvTest {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   ? Java  BMC Desktop Edition");
        System.out.println("========================================");
        
        System.out.println("[?] JVM ");
        
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        
        System.out.println("[i] Java --version: " + javaVersion);
        System.out.println("[i] shop: " + javaVendor);
        System.out.println("[i] OS: " + osName + " (" + osArch + ")");
        
        Runtime runtime = Runtime.getRuntime();
        long maxMB = runtime.maxMemory() / (1024 * 1024);
        long totalMB = runtime.totalMemory() / (1024 * 1024);
        long freeMB = runtime.freeMemory() / (1024 * 1024);
        
        System.out.println("[i] Xmx: " + maxMB + " MB");
        System.out.println("[i] RAM1: " + totalMB + " MB");
        System.out.println("[i] RAM2: " + freeMB + " MB");
        
        String encoding = System.getProperty("file.encoding");
        System.out.println("[i] ASIS: " + encoding);
        if (!encoding.toLowerCase().contains("utf")) {
            System.out.println("[!] WARN:noUTF-8");
        }
        
        System.out.println("========================================");
        System.out.println("? OK");
        System.out.println("? if minecraft fall-check JDK 17/21");
        System.out.println("========================================");
    }
}