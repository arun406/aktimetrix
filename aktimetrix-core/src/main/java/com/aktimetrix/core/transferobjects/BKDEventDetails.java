package com.aktimetrix.core.transferobjects;

import lombok.Data;

import java.util.List;

@Data
public class BKDEventDetails {
    private List<Itinerary> itineraries;
}
