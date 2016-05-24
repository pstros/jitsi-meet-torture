FROM java:7-jdk-alpine

# this needs to be taken out. Its just faking out ant right now
ENV DISPLAY=:2
ENV ANT_VERSION 1.9.6
RUN wget -q http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf /apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mkdir -p /opt && \
    mv /apache-ant-${ANT_VERSION} /opt/ant && \
    rm /apache-ant-${ANT_VERSION}-bin.tar.gz
ENV ANT_HOME /opt/ant
ENV PATH ${PATH}:/opt/ant/bin

COPY . /torture

WORKDIR /torture