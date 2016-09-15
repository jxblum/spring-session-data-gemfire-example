package example.support;

/**
 * The NumberUtils class is an abstract utility class containing support for {@link Number java.lang.Numbers}.
 *
 * @author John Blum
 * @see java.lang.Number
 * @since 1.0.0
 */
@SuppressWarnings("unused")
public abstract class NumberUtils {

	public static int intValue(Number value) {
		return value.intValue();
	}

	public static int nullSafeIntValue(Number value) {
		return (value != null ? value.intValue() : 0);
	}
}
