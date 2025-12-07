package com.merigaumata.user.util;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class CreatePageable {

    private CreatePageable() {}
    /**
     * Converts raw query parameters into a Spring Pageable object.
     * This handles the Pageable mapping since the generator output does not do it automatically.
     */
    public static Pageable createPageable(Integer page, Integer size, List<String> sort) {
        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            // Logic to parse "property,direction" strings into Sort objects
            List<Sort.Order> orders = sort.stream()
                    .map(s -> {
                        String[] parts = s.split(",");
                        String property = parts[0];
                        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                        return new Sort.Order(direction, property);
                    })
                    .toList();
            sorting = Sort.by(orders);
        }

        // Use default values if Spring's internal mechanism isn't providing them
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        return PageRequest.of(pageNumber, pageSize, sorting);
    }
}
