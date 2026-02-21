package es.facite.csvdbq.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class CsvDecimalFormat {
	private static final CsvDecimalFormat CDF_POINT = new CsvDecimalFormat(true);
	private static final CsvDecimalFormat CDF_COMMA = new CsvDecimalFormat(false);

	private static final double DOUBLE_MIN_STD = 1e-17;
	private static final double DOUBLE_MAX_STD = 1e17;
	private final DecimalFormatSymbols decimalFormatSymbols;
	private final DecimalFormat decimalFormatStandard;
	private final DecimalFormat decimalFormatNonStandard;
	private final DecimalFormat decimalFormatGrouped;

	public CsvDecimalFormat(boolean decimalSeparatorPoint) {
		decimalFormatSymbols = new DecimalFormatSymbols();
		decimalFormatSymbols.setDecimalSeparator(decimalSeparatorPoint ? '.' : ',');
		decimalFormatSymbols.setGroupingSeparator(decimalSeparatorPoint ? ',' : '.');
		// Números en valor absoluto de rango [1e-17, 1e17)
		decimalFormatStandard = new DecimalFormat("0.#################", decimalFormatSymbols);
		// Números en valor absoluto de rango (0, 1e-17) ∪ [1e17, ∞)
		decimalFormatNonStandard = new DecimalFormat("0.#################E000", decimalFormatSymbols);
		// Formato numérico agrupado: 1234567.89 => 1,234,567.89
		decimalFormatGrouped = new DecimalFormat("#,###.00", decimalFormatSymbols);
	}

	/**
	 * Devuelve la representación de cadena de un número en función del
	 * separador decimal usado. 
	 * @param dbl
	 * @return
	 */
	public String format(double dbl) {
		double y = Math.abs(dbl);
		if (y >= DOUBLE_MIN_STD && y < DOUBLE_MAX_STD) {
			return decimalFormatStandard.format(dbl);
		} else {
			return decimalFormatNonStandard.format(dbl);
		}
	}

	/**
	 * Devuelve la representación de cadena agrupada de un número en función del
	 * separador decimal usado. Por ejemplo, si el separador decimal es la coma:
	 * 1234567.89 => 1.234.567,89
	 * @param dbl
	 * @return
	 */
	public String grouped(double dbl) {
		return decimalFormatGrouped.format(dbl);
	}

	/**
	 * Devuelve un objeto <code>CsvDecimalFormat</code> en función del separador decimal usado.
	 * @param decimalSeparatorPoint
	 * @return
	 */
	public static CsvDecimalFormat of(boolean decimalSeparatorPoint) {
		if (decimalSeparatorPoint) {
			return CDF_POINT;
		} else {
			return CDF_COMMA;
		}
	}
}
