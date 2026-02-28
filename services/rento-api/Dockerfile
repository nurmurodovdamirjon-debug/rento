FROM mcr.microsoft.com/dotnet/sdk:8.0-alpine AS build
WORKDIR /src
COPY Rento.sln ./
COPY src/Rento.Core/Rento.Core.csproj src/Rento.Core/
COPY src/Rento.Application/Rento.Application.csproj src/Rento.Application/
COPY src/Rento.Infrastructure/Rento.Infrastructure.csproj src/Rento.Infrastructure/
COPY src/Rento.Api/Rento.Api.csproj src/Rento.Api/
RUN dotnet restore src/Rento.Api/Rento.Api.csproj
COPY . .
RUN dotnet publish src/Rento.Api/Rento.Api.csproj -c Release -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:8.0-alpine
RUN apk add --no-cache tzdata
WORKDIR /app
COPY --from=build /app/publish .
EXPOSE 5000
ENV ASPNETCORE_URLS=http://+:5000
ENTRYPOINT ["dotnet", "Rento.Api.dll"]
