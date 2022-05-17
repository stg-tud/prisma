# Base image
FROM eclipse-temurin:11

# Get dependencies of npm
RUN apt-get update && \
    apt-get install -y g++ make python3 nodejs npm cloc bash wget && \
    rm -rf /var/lib/apt/lists/*

# Install truffle and ganache
RUN npm install -g ganache-cli truffle
# dont need this? saves ca. 130MB
#RUN npm install -g n
#RUN n 10.19.0

# Install scala and sbt
ENV SCALA_VERSION 2.13.8
ENV SBT_VERSION 1.5.8
ENV SOLC_VERSION 0.8.4
RUN cd /tmp && \
    wget -q "https://downloads.lightbend.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" -O - | tar xz && \
    mkdir "/usr/share/scala" && \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "/usr/share/scala" && \
    ln -s "/usr/share/scala/bin/"* "/usr/bin/" && \
    wget -q "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" -O - | tar xz && \
    mv "/tmp/sbt/bin/"* "/usr/bin" && \
    wget -q https://github.com/ethereum/solidity/releases/download/v${SOLC_VERSION}/solc-static-linux && \
    chmod +x solc-static-linux && \
    mv solc-static-linux /usr/bin

# Do one compilation and get dependencies
RUN mkdir /tmp/PrismaFiles
COPY ./PrismaFiles /tmp/PrismaFiles
RUN cd /tmp/PrismaFiles/CompilerCode && \
    sh doCompile.sh && \
    rm /tmp/PrismaFiles -r

# dont need this anymore? saves ca. 200MB
RUN apt-get remove -y g++ make python3 && \
    apt-get -y clean autoclean && \
    apt-get -y autoremove

# move into working directory
WORKDIR /app/CompilerCode

# Execute
CMD ["sh", "doCompileRunMeasureCount.sh"]
