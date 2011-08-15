/*
 * Copyright 2011 Leonid Maslov<leonidms@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blogspot.leonardinius.rest;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A JAXB representation of an {@link ErrorCollection} useful for returning via JSON or XML.
 *
 * @since v4.2
 */
@SuppressWarnings({"UnusedDeclaration"})
@XmlRootElement
public class ErrorCollection
{
// ------------------------------ FIELDS ------------------------------

    /**
     * Generic error messages
     */
    @XmlElement
    private Collection<String> errorMessages = new ArrayList<String>();

    @XmlElement
    private Map<String, String> errors = new HashMap<String, String>();

// -------------------------- STATIC METHODS --------------------------

    /**
     * Returns a new ErrorCollection containing a single error message.
     *
     * @param messages an array of Strings containing error messages
     * @return a new ErrorCollection
     */
    public static ErrorCollection of(String... messages)
    {
        Builder b = builder();
        for (int i = 0; messages != null && i < messages.length; i++)
        {
            b.addErrorMessage(messages[i]);
        }

        return b.build();
    }

    /**
     * Returns a new builder. The generated builder is equivalent to the builder created by the {@link ErrorCollection.Builder#newBuilder()} method.
     *
     * @return a new Builder
     */
    public static Builder builder()
    {
        return Builder.newBuilder();
    }

    /**
     * Returns a new ErrorCollection containing all the errors contained in the input error collection.
     *
     * @param errorCollection a ErrorCollection
     * @return a new ErrorCollection
     */
    public static ErrorCollection of(ErrorCollection errorCollection)
    {
        return builder().addErrorCollection(errorCollection).build();
    }

// --------------------------- CONSTRUCTORS ---------------------------

    @SuppressWarnings({"UnusedDeclaration", "unused"})
    private ErrorCollection()
    {
    }

    private ErrorCollection(Collection<String> errorMessages)
    {
        this.errorMessages.addAll(checkNotNull(errorMessages, "errorMessages"));
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Collection<String> getErrorMessages()
    {
        return errorMessages;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(final Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

// -------------------------- OTHER METHODS --------------------------

    @SuppressWarnings("unchecked")
    private void addErrorCollection(ErrorCollection errorCollection)
    {
        errorMessages.addAll(checkNotNull(errorCollection, "errorCollection").getErrorMessages());

        if (errorCollection.errors != null)
        {
            errors.putAll(errorCollection.errors);
        }
    }

    private void addErrorMessage(String errorMessage)
    {
        errorMessages.add(errorMessage);
    }

    public boolean hasAnyErrors()
    {
        return !errorMessages.isEmpty() && !errors.isEmpty();
    }

// -------------------------- INNER CLASSES --------------------------

    /**
     * Builder used to create a new immutable error collection.
     */
    public static class Builder
    {
        private ErrorCollection errorCollection;

        public static Builder newBuilder()
        {
            return new Builder(Collections.<String>emptyList());
        }

        public static Builder newBuilder(Set<String> errorMessages)
        {
            return new Builder(checkNotNull(errorMessages, "errorMessages"));
        }

        public static Builder newBuilder(ErrorCollection errorCollection)
        {
            return new Builder(
                    checkNotNull(errorCollection, "errorCollection")
                            .getErrorMessages());
        }

        Builder(Collection<String> errorMessages)
        {
            this.errorCollection = new ErrorCollection(errorMessages);
        }

        public Builder addErrorCollection(ErrorCollection errorCollection)
        {
            this.errorCollection.addErrorCollection(
                    checkNotNull(errorCollection, "errorCollection"));
            return this;
        }

        public Builder addErrorMessage(String errorMessage)
        {
            this.errorCollection.addErrorMessage(checkNotNull(errorMessage, "errorMessage"));
            return this;
        }

        public ErrorCollection build()
        {
            return this.errorCollection;
        }
    }
}
