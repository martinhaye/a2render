import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.webcodepro.applecommander.storage.DirectoryEntry;
import com.webcodepro.applecommander.storage.Disk;
import com.webcodepro.applecommander.storage.DiskFullException;
import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FormattedDisk;

/*
 * This class uses AppleCommander's command-line interface to extract an entire
 * set of files and directories from an image, or build a whole image from
 * files and directories.
 * 
 * Has specific hard-coded addresses for Lawless Legends files; should fix this
 * at some point to use a metadata configuration file or something like that.
 */
public class A2copy
{
  /*
   * Main command-line driver
   */
  public static void main(String[] args)
    throws IOException, DiskFullException 
  {
    try
    {
      if (args[0].equals("-extract")) {
        extractImg(args[1], args[2]);
        return;
      }
      if (args[0].equals("-create")) {
        createImg(args[1], args[2]);
        return;
      }
    }
    catch (ArrayIndexOutOfBoundsException e)
    { }
    System.err.format("Usage: A2copy [-extract imgFile targetDir] | [-create imgFile sourceDir]\n");
    System.exit(1);
  }
  
  /**
   * Helper to delete a file or directory and all its descendants.
   * @throws IOException        if something goes wrong
   */
  static void delete(File f) throws IOException {
    if (f.isDirectory()) {
      for (File c : f.listFiles())
        delete(c);
    }
    if (!f.delete())
      throw new FileNotFoundException("Failed to delete file: " + f);
  }
  
  /**
   * Extract all the files and directories from an image file.
   * @throws IOException        if something goes wrong
   */
  @SuppressWarnings("unchecked")
  static void extractImg(String imgPath, String targetPath) throws IOException
  {
    // Ensure the target directory is empty first.
    File targetDir = new File(targetPath);
    if (targetDir.exists()) {
      System.out.format("Note: %s will be overwritten. Continue? ", targetPath);
      System.out.flush();
      String response = new BufferedReader(new InputStreamReader(System.in)).readLine();
      if (!response.toLowerCase().startsWith("y"))
        return;
      delete(targetDir);
    }

    // Open the image file and process disks inside it.
    Disk disk = new Disk(imgPath);
    for (FormattedDisk fd : disk.getFormattedDisks())
      extractFiles((List<FileEntry>)fd.getFiles(), targetDir);
  }

  /**
   * Helper for file/directory extraction.
   * @param files               set of files to extract
   * @param targetDir           where to put them
   * @throws IOException        if something goes wrong
   */
  @SuppressWarnings("unchecked")
  static void extractFiles(List<FileEntry> files, File targetDir) throws IOException
  {
    // Ensure the target directory exists
    targetDir.mkdir();
    
    // Process each entry in the list
    for (FileEntry e : files)
    {
      // Skip deleted things
      if (e.isDeleted())
        continue;
      
      // Recursively process sub-directories
      if (e.isDirectory()) {
        File subDir = new File(targetDir, e.getFilename());
        extractFiles(((DirectoryEntry)e).getFiles(), subDir);
        continue;
      }
      
      // Process regular files.
      byte[] data = e.getFileData();
      // Hi to lo ASCII translation
      if (e.getFilename().endsWith(".S"))
        data = merlinSrcToAscii(data);
      FileOutputStream out = new FileOutputStream(new File(targetDir, e.getFilename()));
      out.write(data);
      out.close();
    }
  }
  
  /**
   * Creates an image file using dirs/files from the filesystem.
   * @param imgPath             name of the image file
   * @param srcDirPath          directory containing files and subdirs
   * @throws DiskFullException  if the image file fills up
   * @throws IOException        if something else goes wrong
   */
  static void createImg(String imgPath, String srcDirPath)
    throws IOException, DiskFullException 
  {
    // Alert the user that we're going to blow away the image.
    File imgFile = new File(imgPath);
    if (imgFile.exists()) {
      System.out.format("Note: %s will be overwritten. Continue? ", imgPath);
      System.out.flush();
      String response = new BufferedReader(new InputStreamReader(System.in)).readLine();
      if (!response.toLowerCase().startsWith("y"))
        return;
      delete(imgFile);
    }
    
    // Re-create the image file with a blank image.
    File emptyFile = new File(imgFile.getParent(), "empty.2mg.gz");
    if (!emptyFile.canRead())
      throw new IOException(String.format("Cannot open template for empty image '%s'", emptyFile.toString()));
    InputStream in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(emptyFile)));
    OutputStream out = new BufferedOutputStream(new FileOutputStream(imgFile));
    byte[] buf = new byte[1024];
    while (true) {
      int nRead = in.read(buf);
      if (nRead < 0)
        break;
      out.write(buf, 0, nRead);
    }
    in.close();
    out.close();

    // Open the empty image file.
    Disk disk = new Disk(imgPath);
    FormattedDisk fd = disk.getFormattedDisks()[0];
    
    // And fill it up.
    insertFiles(fd, fd, new File(srcDirPath));
  }

  /**
   * Helper for image creation.
   * 
   * @param fd                  disk to insert files into
   * @param targetDir           directory within the disk
   * @param srcDir              filesystem directory to read
   * @throws DiskFullException  if the image file fills up
   * @throws IOException        if something else goes wrong
   */
  private static void insertFiles(FormattedDisk fd, DirectoryEntry targetDir, File srcDir)
    throws DiskFullException, IOException 
  {
    // Process each file in the source directory
    for (File srcFile : srcDir.listFiles())
    {
      if (srcFile.isDirectory()) {
        DirectoryEntry subDir = targetDir.createDirectory(srcFile.getName().toUpperCase());
        insertFiles(fd, subDir, srcFile);
        continue;
      }
      
      // Create a new entry on the filesystem for this file.
      FileEntry ent = targetDir.createFile();
      String srcName = srcFile.getName().toUpperCase();
      ent.setFilename(srcName);
      
      // Set the file type
      if (srcName.equals("PRODOS") || srcName.endsWith(".SYSTEM"))
        ent.setFiletype("SYS");
      else if (srcName.equals("STARTUP"))
        ent.setFiletype("BAS");
      else if (srcName.endsWith(".S"))
        ent.setFiletype("TXT");
      else
        ent.setFiletype("BIN");
      
      // Set the address if necessary
      if (ent.needsAddress()) {
        if (srcName.equals("STARTUP"))
          ent.setAddress(0x801);
        else if (srcName.equals("COPYIIPL.SYSTEM"))
          ent.setAddress(0x1400);
        else if (srcName.equals("ED.16"))
          ent.setAddress(0x9d60);
        else if (srcName.equals("ED"))
          ent.setAddress(0x9db6);
        else if (srcName.equals("SHELL"))
          ent.setAddress(0x300);
        else
          ent.setAddress(0x2000);
      }
      
      // Copy the file data
      FileInputStream in = new FileInputStream(srcFile);
      byte[] buf = new byte[(int) srcFile.length()];
      int nRead = in.read(buf);
      if (nRead != srcFile.length())
        throw new IOException(String.format("Error reading file '%s'", srcFile.toString()));
      
      // Translate between hi and lo ASCII
      if (srcName.endsWith(".S"))
        buf = asciiToMerlinSrc(buf);
      ent.setFileData(buf);
      
      // And save the new entry.
      fd.save();
    }
  }

  /**
   * Translates Merlin source code to usable code in the regular ASCII world.
   * Performs weird-space to tab translation, and hi bit conversion.
   * 
   * @param buf         data to translate
   * @return            translated data
   */
  static byte[] merlinSrcToAscii(byte[] buf) 
  {
    ByteArrayOutputStream ba = new ByteArrayOutputStream(buf.length);
    PrintWriter out = new PrintWriter(ba);
    boolean inComment = false;
    for (byte b : buf)
    {
      // Handle newlines
      if (b == (byte)0x8d) {
        out.println();
        inComment = false;
      }
      else {
        char c = (char)(b & 0x7f);
        // Tabs outside comments
        if (c == ';' || c == '*')
          inComment = true;
        if (c == '\n')
          throw new RuntimeException("Newline slipped through");
        if (c == ' ' && !inComment)
          out.write('\t');
        else
          out.write(c);
      }
    }
    out.flush();
    return ba.toByteArray();
  }
  
  /**
   * Transforms regular ASCII with tabs to Merlin source code. Handles
   * tab to weird space translation, and hi-bit addition.
   * 
   * @param buf         data to translate
   * @return            translated data
   */
  static byte[] asciiToMerlinSrc(byte[] buf) 
  {
    ByteArrayOutputStream ba = new ByteArrayOutputStream(buf.length);
    boolean inComment = false;
    for (int i=0; i<buf.length; i++) 
    {
      // Newline translation
      if (buf[i] == '\r') {
        ba.write(0x8d);
        if (i+1 < buf.length && buf[i+1] == '\n')
          ++i;
        inComment = false;
      }
      else if (buf[i] == '\n') {
        ba.write(0x8d);
        if (i+1 < buf.length && buf[i+1] == '\r')
          ++i;
        inComment = false;
      }
      // Tabs outside comments
      else if (buf[i] == '\t')
        ba.write(0xA0);
      else if (buf[i] == ' ' && inComment)
        ba.write(0x20);
      else if (buf[i] == 0)
        ba.write(0);
      else {
        if (buf[i] == ';' || buf[i] == '*')
          inComment = true;
        ba.write((((int)buf[i]) & 0xFF) | 0x80);
      }
    }
    return ba.toByteArray();
  }
}
