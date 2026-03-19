package com.turaf.common.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ValueObjectTest {
    
    private static class Address extends ValueObject {
        private final String street;
        private final String city;
        private final String zipCode;
        
        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
        
        @Override
        protected List<Object> getEqualityComponents() {
            return Arrays.asList(street, city, zipCode);
        }
        
        public String getStreet() {
            return street;
        }
        
        public String getCity() {
            return city;
        }
        
        public String getZipCode() {
            return zipCode;
        }
    }
    
    @Test
    void testValueObjectCreation() {
        Address address = new Address("123 Main St", "Springfield", "12345");
        
        assertThat(address.getStreet()).isEqualTo("123 Main St");
        assertThat(address.getCity()).isEqualTo("Springfield");
        assertThat(address.getZipCode()).isEqualTo("12345");
    }
    
    @Test
    void testValueObjectEquality_SameComponents() {
        Address address1 = new Address("123 Main St", "Springfield", "12345");
        Address address2 = new Address("123 Main St", "Springfield", "12345");
        
        assertThat(address1).isEqualTo(address2);
        assertThat(address1.hashCode()).isEqualTo(address2.hashCode());
    }
    
    @Test
    void testValueObjectEquality_DifferentComponents() {
        Address address1 = new Address("123 Main St", "Springfield", "12345");
        Address address2 = new Address("456 Oak Ave", "Springfield", "12345");
        
        assertThat(address1).isNotEqualTo(address2);
    }
    
    @Test
    void testValueObjectEquality_SameInstance() {
        Address address = new Address("123 Main St", "Springfield", "12345");
        
        assertThat(address).isEqualTo(address);
    }
    
    @Test
    void testValueObjectEquality_Null() {
        Address address = new Address("123 Main St", "Springfield", "12345");
        
        assertThat(address).isNotEqualTo(null);
    }
    
    @Test
    void testValueObjectEquality_DifferentClass() {
        Address address = new Address("123 Main St", "Springfield", "12345");
        String notAValueObject = "123 Main St";
        
        assertThat(address).isNotEqualTo(notAValueObject);
    }
    
    @Test
    void testToString() {
        Address address = new Address("123 Main St", "Springfield", "12345");
        
        assertThat(address.toString()).contains("Address");
        assertThat(address.toString()).contains("components");
    }
}
