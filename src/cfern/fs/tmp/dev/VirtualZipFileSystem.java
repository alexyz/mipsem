/**
 * Cfern, a Mips/Unix/C emulator for Java
 * Alexander Slack alexslack56@hotmail.com
 */

package cfern.fs.tmp.dev;
import java.io.*;
import java.util.zip.*;

import cfern.Driver;
import cfern.fs.*;
import cfern.fs.tmp.*;
import cfern.io.FileDescInputStream;

/**
 * A file system based on VirtualFileSystem.
 * Files are pre loaded from zip file on virtual file system.
 */
public class VirtualZipFileSystem extends TempFileSystem {
  
  private static final String name = "vzip";
  
  /**
   * Factory for this file system
   */
  public static final Factory vzipfac = new Factory() {
    public FileSystem newInstance(String mount, String dev) throws IOException {
      return new VirtualZipFileSystem(mount, dev);
    }
    public String name() {
      return name;
    }
  };

  /**
   * Open zip file on virtual file system. This is less efficient than
   * NativeZipFileSystem because it loads everything from the zip file in one go
   * rather than dynamically.
   */
  protected VirtualZipFileSystem(String mount, String upath) throws IOException {
    super(mount, upath);
    FileDesc fd = get().openex(upath);
    
    // the only way to open a non native zip file is to provide an input stream.
    InputStream is = new FileDescInputStream(fd);
    ZipInputStream zis = new ZipInputStream(is);
    
    ZipEntry ze;
    while ((ze = zis.getNextEntry()) != null) {
      Driver.opt().info("VZip: read " + ze);
      // ZipInputStream.read(...)
    }
  }
  
  public String getShortName() {
    return name;
  }
  
}
