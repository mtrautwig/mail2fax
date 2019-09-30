package de.trautwig.fax.mail2fax;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FaxAddressTest {

    @Test
    public void normalizeNumberStartingWith0() {
        assertThat(FaxAddress.normalizePhoneNumber("07259123456")).isEqualTo("+497259123456");
    }

    @Test
    public void normalizeNumberStartingWith00() {
        assertThat(FaxAddress.normalizePhoneNumber("00497259123456")).isEqualTo("00497259123456");
    }

    @Test
    public void normalizeNumberStartingWithCountryCode() {
        assertThat(FaxAddress.normalizePhoneNumber("+497259123456")).isEqualTo("+497259123456");
    }


    @Test(expected = IllegalArgumentException.class)
    public void normalizeNumberStartingInvalid() {
        FaxAddress.normalizePhoneNumber("123456");
    }
}
