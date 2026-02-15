/**
 * Test ma'lumotlar (fixtures) — barcha acceptance testlar uchun
 */

export const TEST_PHONES = {
  tenant: '+998901234567',
  landlord: '+998901234568',
  admin: '+998901234569',
  newUser: '+998909999999',
  blocked: '+998900000001',
};

export const TEST_OTP = '123456';

export const VALID_LISTING = {
  type: 'apartment',
  dealType: 'rent',
  city: 'tashkent',
  district: 'Chilonzor',
  address: 'Chilonzor 7-kvartal, 14-uy',
  landmark: 'Chilonzor metro',
  latitude: 41.2867,
  longitude: 69.2072,
  rooms: 2,
  floor: 4,
  totalFloors: 9,
  areaSqm: 55.0,
  price: 5500000,
  currency: 'UZS',
  priceNegotiable: true,
  hasFurniture: true,
  hasAppliances: true,
  hasInternet: true,
  hasParking: false,
  hasConditioner: true,
  allowsPets: false,
  allowsChildren: true,
  utilitiesIncluded: false,
  depositAmount: 5500000,
  title: 'Chilonzorda 2 xonali kvartira ijaraga',
  description: '2 xonali kvartira, yaxshi tamirli, metroga yaqin. Oila uchun qulay.',
};

export const MINIMAL_LISTING = {
  type: 'apartment',
  city: 'tashkent',
  price: 3000000,
  currency: 'UZS',
  title: 'Test e\'lon',
};

export const INVALID_LISTING_NO_PRICE = {
  type: 'apartment',
  city: 'tashkent',
  title: 'Narxsiz e\'lon',
};

export const SEARCH_FILTERS = {
  fullFilter: {
    city: 'tashkent',
    type: 'apartment',
    rooms: 2,
    minPrice: 3000000,
    maxPrice: 8000000,
  },
  textSearch: {
    query: 'Chilonzor mebelli',
  },
  emptyFilter: {
    city: 'nonexistentcity',
    type: 'castle',
  },
};

export const CHAT_DATA = {
  initialMessage: 'Salom! Bu uy hali bandmi?',
  replyMessage: 'Salom! Ha, hali bo\'sh. Qachon ko\'rishni xohlaysiz?',
  secondMessage: 'Ertaga tushdan keyin kelishim mumkinmi?',
};

export const REPORT_DATA = {
  spam: {
    reason: 'spam',
    description: 'Bu e\'lon takroriy spam e\'lon',
  },
  inappropriate: {
    reason: 'inappropriate_content',
    description: 'Noqonuniy kontentli e\'lon',
  },
  fraud: {
    reason: 'fraud',
    description: 'Firibgarlik: haqiqiy manzil yo\'q',
  },
};

export const PROFILE_UPDATE = {
  fullName: 'Test Foydalanuvchi',
  email: 'test@rento.uz',
  language: 'uz',
};
