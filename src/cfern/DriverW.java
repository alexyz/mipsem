package cfern;

import static cfern.Driver.*;

import java.io.File;

import cfern.fs.FileSystem;
import cfern.gui.SwingConsole;
import cfern.sys.Constants;

/**
 * minimal main class for jar file
 */
public class DriverW {
	
	public static void main (String[] args) {
		putenv("PATH=/bin:/usr/sbin");
	    putenv("HOME=/");
	    putenv("CWD=/");
	    putenv("PWD=/");
	    putenv("USER=root");
	    opt.warn = true;
	    opt.info = true;
	    opt.init(env);
	    Constants.load("glibc.txt");
	    FileSystem vfs = FileSystem.get();
	    vfs.mount("tmp", null, "/");
	    vfs.mkdir("/dev", 0, 0);
	    vfs.mount("dev", null, "/dev");
	    vfs.mkdir("/usr", 0, 0);
	    vfs.mount("nat", System.getProperty("user.dir"), "/usr");
	    vfs.mkdir("/proc", 0, 0);
	    vfs.mount("proc", null, "/proc");
	    if (new File("C:\\").exists()) {
	      vfs.mkdir("/c", 0, 0);
	      vfs.mount("nat", "C:\\", "/c");
	    }
	    vfs.mkdir("/etc", 0, 0);
	    vfs.mount("etc", null, "/etc");
	    vfs.mkdir("/bin", 0, 0);
	    SwingConsole.getInstance();
	}
	
}
