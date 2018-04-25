/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.internal.firmware;

import static org.eclipse.smarthome.core.thing.Thing.PROPERTY_MODEL_ID;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link Firmware}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - FirmwareUID is replaced by ThingTypeUID and firmware version
 */
@NonNullByDefault
public final class FirmwareImpl implements Firmware {

    /** The key for the requires a factory reset property. */
    public static final String PROPERTY_REQUIRES_FACTORY_RESET = "requiresFactoryReset";

    private final Logger logger = LoggerFactory.getLogger(FirmwareImpl.class);

    private final ThingTypeUID thingTypeUID;
    private final @Nullable String vendor;
    private final @Nullable String model;
    private final boolean modelRestricted;
    private final @Nullable String description;
    private final String version;
    private final @Nullable String prerequisiteVersion;
    private final @Nullable String changelog;
    private final @Nullable URL onlineChangelog;
    private final @Nullable transient InputStream inputStream;
    private final @Nullable String md5Hash;
    private final Map<String, String> properties;

    private transient byte @Nullable [] bytes;

    private final Version internalVersion;
    private final @Nullable Version internalPrerequisiteVersion;

    /**
     * Constructs new firmware by the given meta information.
     *
     * @param thingTypeUID thing type UID, that this firmware is associated with (not null)
     * @param vendor the vendor of the firmware (can be null)
     * @param model the model of the firmware (can be null)
     * @param modelRestricted whether the firmware is restricted to a particular model
     * @param description the description of the firmware (can be null)
     * @param version the version of the firmware (not null)
     * @param prerequisiteVersion the prerequisite version of the firmware (can be null)
     * @param changelog the changelog of the firmware (can be null)
     * @param onlineChangelog the URL the an online changelog of the firmware (can be null)
     * @param inputStream the input stream for the binary content of the firmware (can be null)
     * @param md5Hash the MD5 hash value of the firmware (can be null)
     * @param properties the immutable properties of the firmware (can be null)
     *
     * @throws IllegalArgumentException if the ThingTypeUID or the firmware version are null
     */
    public FirmwareImpl(ThingTypeUID thingTypeUID, @Nullable String vendor, @Nullable String model,
            boolean modelRestricted, @Nullable String description, String version, @Nullable String prerequisiteVersion,
            @Nullable String changelog, @Nullable URL onlineChangelog, @Nullable InputStream inputStream,
            @Nullable String md5Hash, @Nullable Map<String, String> properties) {
        ParameterChecks.checkNotNull(thingTypeUID, "ThingTypeUID");
        this.thingTypeUID = thingTypeUID;
        ParameterChecks.checkNotNullOrEmpty(version, "Firmware version");
        this.version = version;
        this.vendor = vendor;
        this.model = model;
        this.modelRestricted = modelRestricted;
        this.description = description;
        this.prerequisiteVersion = prerequisiteVersion;
        this.changelog = changelog;
        this.onlineChangelog = onlineChangelog;
        this.inputStream = inputStream;
        this.md5Hash = md5Hash;
        this.properties = Collections.unmodifiableMap(properties != null ? properties : Collections.emptyMap());
        this.internalVersion = new Version(this.version);
        this.internalPrerequisiteVersion = this.prerequisiteVersion != null ? new Version(this.prerequisiteVersion)
                : null;
    }

    @Override
    public ThingTypeUID getThingTypeUID() {
        return thingTypeUID;
    }

    @Override
    @Nullable
    public String getVendor() {
        return vendor;
    }

    @Override
    @Nullable
    public String getModel() {
        return model;
    }

    @Override
    public boolean isModelRestricted() {
        return modelRestricted;
    }

    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    @Nullable
    public String getPrerequisiteVersion() {
        return prerequisiteVersion;
    }

    @Override
    @Nullable
    public String getChangelog() {
        return changelog;
    }

    @Override
    @Nullable
    public URL getOnlineChangelog() {
        return onlineChangelog;
    }

    @Override
    @Nullable
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    @Nullable
    public String getMd5Hash() {
        return md5Hash;
    }

    @Override
    public synchronized byte @Nullable [] getBytes() {
        if (inputStream == null) {
            return null;
        }

        if (bytes == null) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");

                try (DigestInputStream dis = new DigestInputStream(inputStream, md)) {
                    bytes = IOUtils.toByteArray(dis);
                } catch (IOException ioEx) {
                    logger.error("Cannot read firmware {}.", this, ioEx);
                    return null;
                }

                byte[] digest = md.digest();

                if (md5Hash != null && digest != null) {
                    StringBuilder digestString = new StringBuilder();
                    for (byte b : digest) {
                        digestString.append(String.format("%02x", b));
                    }

                    if (!md5Hash.equals(digestString.toString())) {
                        bytes = null;
                        throw new IllegalStateException(
                                String.format("Invalid MD5 checksum. Expected %s, but was %s.", md5Hash, digestString));
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                logger.error("Cannot calculate MD5 checksum.", e);
                bytes = null;
                return null;
            }
        }

        return bytes;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean isSuccessorVersion(@Nullable String firmwareVersion) {
        if (firmwareVersion == null) {
            return false;
        }
        return internalVersion.compare(new Version(firmwareVersion)) > 0;
    }

    @Override
    public boolean isPrerequisiteVersion(@Nullable String firmwareVersion) {
        if (firmwareVersion == null || internalPrerequisiteVersion == null) {
            return false;
        }

        return new Version(firmwareVersion).compare(internalPrerequisiteVersion) >= 0;
    }

    @Override
    public boolean isSuitableFor(Thing thing) {
        return hasSameThingType(thing) && hasRequiredModel(thing);
    }

    @Override
    public int compareTo(Firmware firmware) {
        return -internalVersion.compare(new Version(firmware.getVersion()));
    }

    private boolean hasSameThingType(Thing thing) {
        return Objects.equals(this.getThingTypeUID(), thing.getThingTypeUID());
    }

    private boolean hasRequiredModel(Thing thing) {
        if (isModelRestricted()) {
            return Objects.equals(this.getModel(), thing.getProperties().get(PROPERTY_MODEL_ID));
        } else {
            return true;
        }
    }

    private static class Version {

        private static final int NO_INT = -1;

        private final String[] parts;

        private Version(@Nullable String versionString) {
            if (versionString == null) {
                this.parts = new String[] {};
            } else {
                this.parts = versionString.split("-|_|\\.");
            }
        }

        private int compare(@Nullable Version theVersion) {

            if (theVersion == null) {
                return 1;
            }

            int max = Math.max(parts.length, theVersion.parts.length);

            for (int i = 0; i < max; i++) {
                String partA = i < parts.length ? parts[i] : null;
                String partB = i < theVersion.parts.length ? theVersion.parts[i] : null;

                Integer intA = partA != null && isInt(partA) ? Integer.parseInt(partA) : NO_INT;
                Integer intB = partB != null && isInt(partB) ? Integer.parseInt(partB) : NO_INT;

                if (intA != NO_INT && intB != NO_INT) {
                    if (intA < intB) {
                        return -1;
                    }
                    if (intA > intB) {
                        return 1;
                    }
                } else if (partA == null || partB == null) {
                    if (partA == null) {
                        return -1;
                    }
                    if (partB == null) {
                        return 1;
                    }
                } else {
                    int result = partA.compareTo(partB);
                    if (result != 0) {
                        return result;
                    }
                }
            }

            return 0;
        }

        private boolean isInt(String s) {
            return s.matches("^-?\\d+$");
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changelog == null) ? 0 : changelog.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((md5Hash == null) ? 0 : md5Hash.hashCode());
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + Boolean.hashCode(modelRestricted);
        result = prime * result + ((onlineChangelog == null) ? 0 : onlineChangelog.hashCode());
        result = prime * result + ((prerequisiteVersion == null) ? 0 : prerequisiteVersion.hashCode());
        result = prime * result + ((thingTypeUID == null) ? 0 : thingTypeUID.hashCode());
        result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Firmware other = (Firmware) obj;
        if (changelog == null) {
            if (other.getChangelog() != null) {
                return false;
            }
        } else if (!changelog.equals(other.getChangelog())) {
            return false;
        }
        if (description == null) {
            if (other.getDescription() != null) {
                return false;
            }
        } else if (!description.equals(other.getDescription())) {
            return false;
        }
        if (md5Hash == null) {
            if (other.getMd5Hash() != null) {
                return false;
            }
        } else if (!md5Hash.equals(other.getMd5Hash())) {
            return false;
        }
        if (model == null) {
            if (other.getModel() != null) {
                return false;
            }
        } else if (!model.equals(other.getModel())) {
            return false;
        }
        if (modelRestricted != other.isModelRestricted()) {
            return false;
        }
        if (onlineChangelog == null) {
            if (other.getOnlineChangelog() != null) {
                return false;
            }
        } else if (!onlineChangelog.equals(other.getOnlineChangelog())) {
            return false;
        }
        if (prerequisiteVersion == null) {
            if (other.getPrerequisiteVersion() != null) {
                return false;
            }
        } else if (!prerequisiteVersion.equals(other.getPrerequisiteVersion())) {
            return false;
        }
        if (thingTypeUID == null) {
            if (other.getThingTypeUID() != null) {
                return false;
            }
        } else if (!thingTypeUID.equals(other.getThingTypeUID())) {
            return false;
        }
        if (vendor == null) {
            if (other.getVendor() != null) {
                return false;
            }
        } else if (!vendor.equals(other.getVendor())) {
            return false;
        }
        if (version == null) {
            if (other.getVersion() != null) {
                return false;
            }
        } else if (!version.equals(other.getVersion())) {
            return false;
        }
        if (properties == null) {
            if (other.getProperties() != null) {
                return false;
            }
        } else if (!properties.equals(other.getProperties())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FirmwareImpl [thingTypeUID=" + thingTypeUID + ", vendor=" + vendor + ", model=" + model
                + ", modelRestricted=" + modelRestricted + ", description=" + description + ", version=" + version
                + ", prerequisiteVersion=" + prerequisiteVersion + ", changelog=" + changelog + ", onlineChangelog="
                + onlineChangelog + ", md5Hash=" + md5Hash + ", properties=" + properties + "]";
    }

}
