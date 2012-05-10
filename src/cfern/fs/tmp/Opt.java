package cfern.fs.tmp;

import cfern.sys.Errors;

/**
 * FIXME experimental class
 * Exactly one of file, err and xfsln will be not null.
 */
public class Opt implements Errors {
  
  static final Opt notFound = new Opt(null, enoent, null);
  static final Opt notDir = new Opt(null, enotdir, null);

  public final File file;
  public final String err;
  public final String xfsln;
  
  private Opt(File file, String err, String xfsln) {
    this.file = file;
    this.err = err;
    this.xfsln = xfsln;
  }
  
  static Opt forFile(File file) {
      return file == null ? notFound : new Opt(file, null, null);
  }
  
  static Opt forLink(String path) {
    if (path == null || !path.startsWith("/")) // etc
      throw new RuntimeException();
    return new Opt(null, null, path);
  }

}
