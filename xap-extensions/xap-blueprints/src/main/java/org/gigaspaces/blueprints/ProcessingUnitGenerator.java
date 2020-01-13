package org.gigaspaces.blueprints;

import com.gigaspaces.start.SystemLocations;
import org.jini.rio.boot.PUZipUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class ProcessingUnitGenerator {

    private static Logger logger = Logger.getLogger(ProcessingUnitGenerator.class.getName());

    public static void generate(String name, Map<String, String> properties) throws Exception {
        Blueprint blueprint = BlueprintRepository.getDefault().get(name);

        Path workDir = SystemLocations.singleton().work();
        Path tempDirectory = Files.createTempDirectory(workDir, "blueprint-temp");
        Path target = blueprint.getDefaultTarget(tempDirectory);

        blueprint.generate(target, properties);
        File zipFile = new File(workDir.toString()+"/"+target.getFileName()+".zip");
        if(zipFile.exists()) {
            zipFile.delete();
        }
        PUZipUtils.zip(tempDirectory.toFile(), zipFile);
        logger.info("************************* Target Path: "+target.toString());
    }
}
