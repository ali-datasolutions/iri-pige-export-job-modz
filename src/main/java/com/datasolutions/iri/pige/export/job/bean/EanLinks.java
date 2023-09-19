package com.datasolutions.iri.pige.export.job.bean;

import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

public class EanLinks {

    private final Map<String, Set<String>> links = Maps.newHashMap();

    public EanLinks(Set<String> eans) {
        for (String ean : eans) {
            this.links.put(ean, Sets.newHashSet());
        }
    }

    public Set<String> getEans() {
        Set<String> eans = this.links.keySet();
        return ImmutableSet.copyOf(eans);
    }

    public Set<String> getLinks(String ean) {
        Set<String> links = this.links.get(ean);
        return links != null ? ImmutableSet.copyOf(links) : null;
    }

    public Set<String> putLink(EanLink link) {
        String ean = link.getEan();
        Set<String> links = this.links.get(ean);
        if (links == null) {
            throw new IllegalArgumentException("Links for " + ean + " were not initialized!");
        }
        links.add(link.getLink());
        return ImmutableSet.copyOf(links);
    }

    public long size() {
        return this.links.values().stream().mapToLong(Set::size).sum();
    }

}
