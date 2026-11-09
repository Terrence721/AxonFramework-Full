package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListUtilsTest {

    @Test
    void distinctRemovesDuplicatesPreservingOrder() {
        assertEquals(List.of("a", "b", "c"),
                ListUtils.distinct(List.of("a", "b", "a", "c", "b")));
    }

    @Test
    void distinctReturnsEmptyListForEmptyInput() {
        assertEquals(List.of(), ListUtils.distinct(List.of()));
    }
}