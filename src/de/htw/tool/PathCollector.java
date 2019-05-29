package de.htw.tool;

import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;


/**
 * A basic path visitor that can be used to walk a file tree recursively, and collect all directories and regular files
 * encountered. The visitor will not follow that are not symbolic links, or only reachable by a symbolic link. The resulting
 * paths can be accessed after walking a file tree. Note that instances are not thread safe. However, they can be used
 * subsequently for more than one file tree walk.
 * @see Files#walkFileTree(Path, FileVisitor)
 */
@Copyright(year = 2013, holders = "Sascha Baumeister")
public class PathCollector extends SimpleFileVisitor<Path> {
	private final Set<Path> visitedDirectoryPaths;
	private final Set<Path> visitedFilePaths;
	private final boolean followLinks;


	/**
	 * Creates a new instance that handles links as specified.
	 * @param followLinks whether or not to follow links
	 */
	public PathCollector (final boolean followLinks) {
		this.visitedDirectoryPaths = new HashSet<>();
		this.visitedFilePaths = new HashSet<>();
		this.followLinks = followLinks;
	}


	/**
	 * {@inheritDoc}
	 */
	public FileVisitResult preVisitDirectory (final Path directoryPath, final BasicFileAttributes attributes) {
		if (this.followLinks) {
			if (Files.isDirectory(directoryPath)) {
				this.visitedDirectoryPaths.add(directoryPath);
			}
		} else {
			if (Files.isDirectory(directoryPath, LinkOption.NOFOLLOW_LINKS)) {
				this.visitedDirectoryPaths.add(directoryPath);
			} else {
				return FileVisitResult.SKIP_SUBTREE;
			}
		}
		return FileVisitResult.CONTINUE;
	}


	/**
	 * {@inheritDoc}
	 */
	public FileVisitResult visitFile (final Path filePath, final BasicFileAttributes attributes) {
		if (this.followLinks) {
			if (Files.isRegularFile(filePath)) {
				this.visitedFilePaths.add(filePath);
			}
		} else {
			if (Files.isRegularFile(filePath, LinkOption.NOFOLLOW_LINKS)) {
				this.visitedFilePaths.add(filePath);
			}
		}
		return FileVisitResult.CONTINUE;
	}


	/**
	 * Returns the directory paths visited since the collector was created, or the collection returned was cleared at the time.
	 * @return the directory paths visited
	 */
	public Set<Path> getVisitedDirectoryPaths () {
		return this.visitedDirectoryPaths;
	}


	/**
	 * Returns the file paths visited since the collector was created, or the collection returned was cleared at the time.
	 * @return the file paths visited
	 */
	public Set<Path> getVisitedFilePaths () {
		return this.visitedFilePaths;
	}
}