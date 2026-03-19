package com.turaf.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EntityTest {
    
    private static class TestEntity extends Entity<String> {
        private String name;
        
        public TestEntity(String id, String name) {
            super(id);
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    @Test
    void testEntityCreation() {
        TestEntity entity = new TestEntity("123", "Test");
        
        assertThat(entity.getId()).isEqualTo("123");
        assertThat(entity.getName()).isEqualTo("Test");
    }
    
    @Test
    void testEntityCreation_NullId_ThrowsException() {
        assertThatThrownBy(() -> new TestEntity(null, "Test"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Entity ID cannot be null");
    }
    
    @Test
    void testEntityEquality_SameId() {
        TestEntity entity1 = new TestEntity("123", "Name1");
        TestEntity entity2 = new TestEntity("123", "Name2");
        
        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }
    
    @Test
    void testEntityEquality_DifferentId() {
        TestEntity entity1 = new TestEntity("123", "Test");
        TestEntity entity2 = new TestEntity("456", "Test");
        
        assertThat(entity1).isNotEqualTo(entity2);
    }
    
    @Test
    void testEntityEquality_SameInstance() {
        TestEntity entity = new TestEntity("123", "Test");
        
        assertThat(entity).isEqualTo(entity);
    }
    
    @Test
    void testEntityEquality_Null() {
        TestEntity entity = new TestEntity("123", "Test");
        
        assertThat(entity).isNotEqualTo(null);
    }
    
    @Test
    void testEntityEquality_DifferentClass() {
        TestEntity entity = new TestEntity("123", "Test");
        String notAnEntity = "123";
        
        assertThat(entity).isNotEqualTo(notAnEntity);
    }
    
    @Test
    void testToString() {
        TestEntity entity = new TestEntity("123", "Test");
        
        assertThat(entity.toString()).contains("TestEntity");
        assertThat(entity.toString()).contains("123");
    }
}
