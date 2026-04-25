param(
  [switch]$Apply
)

$ErrorActionPreference = "Stop"

$root = "C:\Users\lauta\Desktop\tp1c2026\backend\src\main\java\com\tacs\tp1c2026\entities"

$renameMap = @{
  "Alerta.java" = "Alert.java"
  "AlertaFiguritaFaltante.java" = "MissingStickerAlert.java"
  "AlertaPropuestaRecibida.java" = "ReceivedProposalAlert.java"
  "AlertaSubastaProxima.java" = "UpcomingAuctionAlert.java"
  "AlertaVisitor.java" = "AlertVisitor.java"
  "Figurita.java" = "Sticker.java"
  "FiguritaColeccion.java" = "CollectionSticker.java"
  "FiguritaFaltante.java" = "MissingSticker.java"
  "GrupoPerfil.java" = "ProfileGroup.java"
  "ItemOfertaSubasta.java" = "AuctionOfferItem.java"
  "OfertaSubasta.java" = "AuctionOffer.java"
  "Perfil.java" = "Profile.java"
  "PropuestaIntercambio.java" = "ExchangeProposal.java"
  "PublicacionIntercambio.java" = "ExchangePublication.java"
  "Subasta.java" = "Auction.java"
  "Usuario.java" = "User.java"
}

$gitAvailable = $false
try {
  git --no-pager rev-parse --is-inside-work-tree | Out-Null
  if ($LASTEXITCODE -eq 0) {
    $gitAvailable = $true
  }
} catch {
  $gitAvailable = $false
}

foreach ($entry in $renameMap.GetEnumerator()) {
  $source = Join-Path $root $entry.Key
  $target = Join-Path $root $entry.Value

  if (!(Test-Path $source)) {
    Write-Host "Skip (missing): $source"
    continue
  }

  if (Test-Path $target) {
    Write-Host "Skip (target exists): $target"
    continue
  }

  if (-not $Apply) {
    Write-Host "DRY RUN: $($entry.Key) -> $($entry.Value)"
    continue
  }

  if ($gitAvailable) {
    git --no-pager mv "$source" "$target"
  } else {
    Rename-Item -Path $source -NewName $entry.Value
  }

  Write-Host "Renamed: $($entry.Key) -> $($entry.Value)"
}

