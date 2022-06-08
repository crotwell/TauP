package edu.sc.seis.TauP;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Properties;

public class PropLoadTest {

    @Test
    public void load() throws IOException {
        Properties props = PropertyLoader.load();
        assertTrue(props.containsKey("taup.model.name"));
        assertEquals("iasp91", props.get("taup.model.name"));
        assertTrue(props.containsKey("taup.maxKmpsLaps"));
    }
}
