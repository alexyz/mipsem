/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp.dev;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import cfern.Driver;
import cfern.fs.*;
import cfern.fs.tmp.*;
import cfern.fs.tmp.File;

/**
 * A file system based on VirtualFileSystem.
 * Files are dynamically loaded from zip file on native file system.
 */
public class NativeZipFileSystem extends TempFileSystem {
  
  private static final String name = "zip";
  
  /**
   * Factory for this file system
   */
  public static final Factory zipfac = new Factory() {
    public FileSystem newInstance(String mount, String dev) {
      return new NativeZipFileSystem(mount, dev);
    }
    public String name() {
      return name;
    }
  };
  
  /**
   * Backing zip file.
   */
  private final ZipFile zip;
  
  /**
   * Open zip file on native file system.
   * ZipFile.getInputStream(ZipEntry)
   */
  protected NativeZipFileSystem(String mount, String npath) {
    super(mount, npath);
    try {
      zip = new ZipFile(npath);
    } catch (IOException e) {
      throw new RuntimeException("could not read native zip", e);
    }
    load();
  }
  
  /**
   * Create a virtual file for each entry in the zip file.
   */
  private void load() {
    Enumeration<? extends ZipEntry> e = zip.entries();
    while (e.hasMoreElements()) {
      final ZipEntry ze = e.nextElement();
      String[] path = FileSystemUtil.split(ze.getName());
      Dir dir = root.getdirfor(path, true);
      
      if (dir == null) {
        Driver.opt().fslog("invalid zip path %s", ze);
        continue;
      }
      
      File f;
      if (ze.isDirectory()) {
        f = new Dir();
      } else {
        f = new SparseReg() {
          protected InputStream content() throws IOException {
            return zip.getInputStream(ze);
          }
        };
      }
      
      String name = path[path.length - 1];
      if (!dir.put(name, f))
        Driver.opt().fslog("file already exists for path %s", ze);
    }
  }
  
  public String getShortName() {
    return name;
  }

} // end of class NativeZipFileSystem
