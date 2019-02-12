
public class Util {

	public static String replaceSuffix(String fileName, String suffix) {
		int index = fileName.indexOf('.', 0);
		if (index != -1) {
			int lastIndex = index;
			while (index != -1) {
				index = fileName.indexOf('.', lastIndex + 1);
				if (index != -1)
					lastIndex = index;
			}
			return fileName.substring(0, lastIndex) + suffix;
		} else {
			return fileName + "suffix";
		}
	}

}