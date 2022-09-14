package cx.rain.synccraft.sync.utility;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

public class FileHelper {
    public static Set<File> enumerateFiles(File file, boolean includeChildDir, Set<File> fileSet) {
        if (file.isDirectory()) {
            if (includeChildDir) {
                for (var child : file.listFiles()) {
                    fileSet.addAll(enumerateFiles(child, includeChildDir, fileSet));
                }
            }

            fileSet.addAll(Arrays.stream(file.listFiles()).toList());
        } else {
            fileSet.add(file);
        }

        return fileSet;
    }

    public static boolean matchesSHA256(File file, String sha256) {
        try {
            var result = DigestUtils.sha256Hex(FileUtils.readFileToByteArray(file));
            return sha256.equalsIgnoreCase(result);
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
