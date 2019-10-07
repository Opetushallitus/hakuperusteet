comment on database hakuperusteet is 'Tietokanta hakemuksen käsittelymaksutiedoille';

-- application_object
comment on table application_object is 'Hakijan hakukohteen tiedot';
comment on column application_object.id is 'Taulun rivin id-tunniste';
comment on column application_object.henkilo_oid is 'Hakijan OID';
comment on column application_object.hakukohde_oid is 'Hakukohteen OID';
comment on column application_object.education_level is 'Hakijan tutkinnon taso';
comment on column application_object.education_country is 'Hakijan tutkinnon suoritusmaa';
comment on column application_object.haku_oid is 'Haun OID';

-- payment
comment on table payment is 'Maksun tiedot';
comment on column payment.id is 'Taulun rivin id-tunniste';
comment on column payment.henkilo_oid is 'Hakijan OID';
comment on column payment.tstamp is 'Maksun aikaleima';
comment on column payment.reference is 'Maksun viite';
comment on column payment.order_number is 'Maksun tilausnumero';
comment on column payment.status is 'Maksun tila';
comment on column payment.paym_call_id is 'Maksun kutsu-id';
comment on column payment.hakemus_oid is 'Hakemuksen OID';
comment on column payment.hakumaksukausi is 'Hakumaksun kausitieto';

-- payment_event
comment on table payment_event is 'Maksutapahtuman tiedot';
comment on column payment_event.id is 'Taulun rivin id-tunniste';
comment on column payment_event.payment_id is 'Maksutapahtuman id-numero';
comment on column payment_event.created is 'Maksutapahtuman luonnin aikaleima';
comment on column payment_event.timestamp is 'Maksutapahtuman aikaleima';
comment on column payment_event.payment_status is 'Maksutapahtuman tila';
comment on column payment_event.check_succeeded is 'Maksutapahtuman tarkistuksen tilatieto';
comment on column payment_event.new_status is 'Maksutapahtuman tila';
comment on column payment_event.old_status is 'Vanha maksutapahtuman tila';

-- synchronization
comment on table synchronization is 'Maksun synkronointitiedot';
comment on column synchronization.id is 'Taulun rivin id-tunniste';
comment on column synchronization.created is 'Rivin luonnin aikaleima';
comment on column synchronization.henkilo_oid is 'Hakijan OID';
comment on column synchronization.haku_oid is 'Haun OID';
comment on column synchronization.hakukohde_oid is 'Hakukohteen OID';
comment on column synchronization.status is 'Synkronoinnin Statustieto';
comment on column synchronization.updated is 'Rivin päivityksen aikaleima';
comment on column synchronization.hakemus_oid is 'Hakemuksen OID';

-- user
comment on table "user" is 'Hakijan tiedot';
comment on column "user".id is 'Taulun rivin id-tunniste';
comment on column "user".henkilo_oid is 'Hakijan OID';
comment on column "user".email is 'Hakijan sähköpostiosoite';
comment on column "user".idpentityid is 'Tunnistustietojen tarjoajan id';
comment on column "user".uilang is 'Hakijan käyttöliittymän kieli';

-- user_details
comment on table user_details is 'Hakijan henkilötiedot';
comment on column user_details.id is 'Taulun rivin id-tunniste';
comment on column user_details.firstname is 'Hakijan etunimi';
comment on column user_details.lastname is 'Hakijan sukunimi';
comment on column user_details.gender is 'Hakijan sukupuoli';
comment on column user_details.birthdate is 'Hakijan syntymäaika';
comment on column user_details.personid is 'Hakijan henkilötunnus';
comment on column user_details.native_language is 'Hakijan äidinkieli';
comment on column user_details.nationality is 'Hakijan kansallisuus';


