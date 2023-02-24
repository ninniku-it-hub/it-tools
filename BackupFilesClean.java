import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

public class BackupFilesClean {
	public static void main(String[] args) {
		long sizeLimit = 128;
		if (args.length >= 2) {
			System.out.println("Source folder path: " + args[0]);
			System.out.println("Destination folder path: " + args[1]);
			if (args.length >= 3 && args[2] != null) {
				sizeLimit = Long.valueOf(args[2]);
			}
			System.out.println("Folder size limit: " + sizeLimit + " GB");
		} else {
			System.out.println("No parameters specified");
			return;
		}

		String sourceFolder = args[0];
		String destFolder = args[1];
		File sourceDir = new File(sourceFolder);
		File destDir = new File(destFolder);
		
		backupFiles(sourceDir, destDir);
		cleanupFolder(destDir, sizeLimit);
	}

	private static void backupFiles(File sourceDir, File destDir) {
		// Set cutoff time to 24 hours ago
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY, -24);
		Date cutoffDate = cal.getTime();

		// Create destination folder if it does not exist
		if (!destDir.exists()) {
			destDir.mkdir();
		}

		// Get all files in the source folder
		File[] files = sourceDir.listFiles();
		int counter = 0;
		// Copy files created in the last 24 hours from source folder to destination
		// folder

		for (File file : files) {
			if (file.isFile() && file.lastModified() >= cutoffDate.getTime()) {
				try {
					@SuppressWarnings("resource")
					FileChannel srcChannel = new FileInputStream(file).getChannel();
					@SuppressWarnings("resource")
					FileChannel destChannel = new FileOutputStream(new File(destDir, file.getName())).getChannel();
					destChannel.transferFrom(srcChannel, 0, srcChannel.size());
					srcChannel.close();
					destChannel.close();
					System.out.println(file.getName() + " was copied");
					counter++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Backup completed. " + counter + " file(s) were successfully copied.");

	}

	private static void cleanupFolder(File destDir, long sizeLimit) {

		long maxFolderSize = sizeLimit * 1024L * 1024L * 1024L; // Maximum folder size in bytes
		// Check if folder exists and is a directory
		File folder = destDir;
		if (!folder.exists() || !folder.isDirectory()) {
			System.out.println("Folder does not exist or is not a directory");
			return;
		}

		// Get all files in folder and sort by last modified time
		File[] files = folder.listFiles();
		if (files == null || files.length == 0) {
			System.out.println("Folder is empty");
			return;
		}

		// Calculate folder size and delete oldest files until folder size is under size
		// limit
		long folderSize = 0;
		try {
			folderSize = calculateFolderSize(Paths.get(destDir.getPath()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Folder size: " + folderSize + " bytes");

		// Delete the oldest file if the folder exceeds the maximum size
		while (folderSize > maxFolderSize) {
			Arrays.sort(files, Comparator.comparingLong(File::lastModified));
			File oldestFile = files[0];

			Path path = Paths.get(oldestFile.getPath());
			try {
				long fileSize = Files.size(path);
				System.out.println("File size: " + fileSize + " bytes");
				if (oldestFile.delete()) {
					folderSize -= fileSize;
					System.out.println("Deleted file: " + oldestFile.getName());
				} else {
					System.out.println("Failed to delete file: " + oldestFile.getName());
				}
			} catch (IOException e) {
				System.err.println("Failed to get file size: " + e.getMessage());
			}
		}
	}

	public static long calculateFolderSize(Path folderPath) throws IOException {
		FolderSizeVisitor visitor = new FolderSizeVisitor();
		Files.walkFileTree(folderPath, visitor);
		return visitor.getSize();
	}

	private static class FolderSizeVisitor extends SimpleFileVisitor<Path> {
		private long size = 0;

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			size += attrs.size();
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			// Handle file visit failure
			return FileVisitResult.CONTINUE;
		}

		public long getSize() {
			return size;
		}
	}
}
