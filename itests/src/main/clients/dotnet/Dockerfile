FROM mcr.microsoft.com/dotnet/sdk:3.1-alpine3.13

COPY . /app/
WORKDIR /app/

RUN dotnet restore
RUN dotnet build

ENTRYPOINT [""]
CMD ["sleep", "600"]