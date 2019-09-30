package de.trautwig.fax.mail2fax;

import java.util.Locale;

/**
 * See RFC 2303 / RFC 2304
 */
public class FaxAddress {
    private static final String DEFAULT_COUNTRY_CODE = "+49";

    private final String serviceSelector = "FAX";
    private String globalPhone;
    private String t33sep;

    public FaxAddress(String address) {
        if (address.matches("[+]?[0-9]+")) {
            globalPhone = normalizePhoneNumber(address);
        } else {
            int rhs = address.indexOf('@');
            if (rhs == -1) {
                throw new IllegalArgumentException(address);
            }
            String pstnAddress = address.substring(0, rhs);
            if (pstnAddress.startsWith("/")) {
                pstnAddress = pstnAddress.substring(1);
            }
            if (pstnAddress.endsWith("/")) {
                pstnAddress = pstnAddress.substring(0, pstnAddress.length() - 1);
            }

            if (!pstnAddress.toUpperCase(Locale.ENGLISH).startsWith(serviceSelector + "=")) {
                throw new IllegalArgumentException(pstnAddress);
            }

            int sep = pstnAddress.indexOf('/');
            if (sep == -1) {
                globalPhone = normalizePhoneNumber(pstnAddress.substring(4));
            } else {
                globalPhone = normalizePhoneNumber(globalPhone.substring(0, sep));
                String qualifType1 = pstnAddress.substring(sep);
                if (qualifType1.startsWith("T33S=")) {
                    t33sep = qualifType1.substring(5);
                }
            }
        }
    }

    public String getServiceSelector() {
        return serviceSelector;
    }

    public String getGlobalPhone() {
        return globalPhone;
    }

    public String getT33sep() {
        return t33sep;
    }

    static String normalizePhoneNumber(String number) {
        number = number.replaceAll("[^+0-9]", "");
        if (number.startsWith("+") || number.startsWith("00")) {
            return number;
        }
        if (number.startsWith("0")) {
            return DEFAULT_COUNTRY_CODE + number.substring(1);
        }
        throw new IllegalArgumentException(number);
    }
}
